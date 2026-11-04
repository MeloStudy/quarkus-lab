package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;

/**
 * What the reminders do when the provider misbehaves. The assertions that matter are made AT THE PROVIDER: how many messages
 * really went out and how many calls it received, not only what the application says about itself.
 */
@QuarkusTest
@QuarkusTestResource(NotificationStub.class)
@TestSecurity(user = "front-desk", roles = "reception")
class ReminderResilienceTest {

    private static final String DUE = "2026-03-08T12:00:00Z"; // after every reminder of the Monday appointments below is due

    @Inject
    TestData data;

    @Inject
    ReminderService reminders;

    long ana;
    long checkup;

    @BeforeEach
    void fixture() {
        data.reset();
        Provider.reset();
        CircuitBreakerMaintenance.get().resetAll(); // the breaker outlives a test: reset, and never assume where it starts
        Clinic.clockAt(Clinic.NOW);
        ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        checkup = Clinic.treatment("Checkup", 30);
    }

    /** Books a patient with this email at 09:00Z + {@code n} half hours on Monday 2026-03-09. Returns the reminder id. */
    private long reminderFor(String email, int n) {
        long patient = Clinic.patient("Patient " + n, email);
        int minutes = n * 30;
        String start = String.format("2026-03-09T%02d:%02d:00Z", 8 + minutes / 60, minutes % 60);
        Clinic.booked(ana, patient, checkup, start);
        return n + 1L; // reminders are numbered like the appointments in a clean database
    }

    private static io.restassured.response.ValidatableResponse reminder() {
        return given().get("/api/reminders").then().statusCode(200);
    }

    @Test
    void aTemporaryFailureIsRetriedAtOnceWithTheSameKeyAndGoesOutOnce() {
        reminderFor("bea@example.com", 0);
        Clinic.clockAt(DUE);
        Provider.failNext(1, 503); // the first call fails without being processed

        ReminderService.Report report = reminders.sendDue();

        assertEquals(1, report.sent());
        assertEquals(2, Provider.calls(), "one failed call and its retry");
        assertEquals(1, Provider.deliveries());
        // one attempt of the application = two HTTP calls: attempts count what the pipeline did, not what the network did
        reminder().body("[0].status", equalTo("SENT")).body("[0].attempts", equalTo(1));
    }

    @Test
    void anAnswerThatIsLostAfterTheProviderDeliveredDoesNotSendTheMessageTwice() {
        reminderFor("amy@lostanswer.test", 0);
        Clinic.clockAt(DUE);

        ReminderService.Report report = reminders.sendDue();

        assertEquals(1, report.sent());
        assertEquals(2, Provider.calls(), "the first call was delivered and its answer lost; the retry asked again");
        assertEquals(1, Provider.deliveries(), "...and the provider recognized the key: ONE message went out");
    }

    @Test
    void aRefusalForGoodIsNotRetriedAndWaitsForAPerson() {
        reminderFor("nobody@invalid.test", 0);
        Clinic.clockAt(DUE);

        ReminderService.Report report = reminders.sendDue();

        assertEquals(1, report.failed());
        assertEquals(1, Provider.calls(), "a 400 is not retried");
        reminder().body("[0].status", equalTo("FAILED")).body("[0].attempts", equalTo(1))
                .body("[0].lastError", containsString("refused the message with 400"));

        // a person fixes the address and asks again
        data.execute("UPDATE patient SET email = 'fixed@example.com'");
        given().post("/api/reminders/1/retry").then().statusCode(200).body("status", equalTo("PENDING")).body("attempts", equalTo(0));
        assertEquals(1, reminders.sendDue().sent());
        assertEquals(1, Provider.deliveries());
        // only a failed reminder can be retried
        given().post("/api/reminders/1/retry").then().statusCode(409).body("code", equalTo("INVALID_TRANSITION"));
    }

    @Test
    void aRefusalForGoodDoesNotCountAgainstTheCircuitBreaker() {
        for (int i = 0; i < 6; i++) {
            reminderFor("nobody" + i + "@invalid.test", i);
        }
        Clinic.clockAt(DUE);

        ReminderService.Report report = reminders.sendDue();

        assertEquals(6, report.failed(), "every one was tried and refused");
        assertEquals(0, report.postponed(), "six 400s in a row did not open the breaker (lab 23: skipOn)");
        assertEquals(6, Provider.calls());
    }

    @Test
    void anOutageOpensTheBreakerAndTheRestOfTheBatchIsPutOffWithoutCountingAnAttempt() {
        for (int i = 0; i < 8; i++) {
            reminderFor("p" + i + "@example.com", i);
        }
        Clinic.clockAt(DUE);
        Provider.failNext(1000, 503);

        ReminderService.Report report = reminders.sendDue();

        assertTrue(report.postponed() > 0, "the breaker opened and stopped the batch: " + report);
        assertTrue(Provider.calls() < 8 * 3, "far fewer calls than 8 reminders x 3 tries: " + Provider.calls());
        assertEquals(8, report.claimed());
        // the put-off ones are NOT at fault: no attempt counted, they wait for the provider to come back
        long untouched = given().get("/api/reminders").then().extract().jsonPath().getList("findAll { it.attempts == 0 }").size();
        assertEquals(report.postponed(), untouched, "postponed reminders keep attempts = 0");
        assertEquals(0, Provider.deliveries());
    }

    @Test
    void whenTheProviderComesBackEverythingIsDeliveredExactlyOnce() throws Exception {
        for (int i = 0; i < 6; i++) {
            reminderFor("p" + i + "@example.com", i);
        }
        Clinic.clockAt(DUE);
        Provider.failNext(1000, 503);
        reminders.sendDue(); // the outage: some retrying, some put off

        Provider.reset(); // healthy again
        Thread.sleep(1200); // the breaker of the tests stays open for 1 second
        for (int round = 1; round <= 8 && Provider.deliveries() < 6; round++) {
            Clinic.clockAt(java.time.Instant.parse(DUE).plus(java.time.Duration.ofHours(round)).toString()); // backoffs and pauses elapse
            reminders.sendDue();
        }

        assertEquals(6, Provider.deliveries(), "everybody was told");
        assertEquals(6, Provider.seen().stream().distinct().count(), "...and nobody twice: " + Provider.seen());
        reminder().body("status", org.hamcrest.Matchers.everyItem(equalTo("SENT")));
    }

    @Test
    void severalInstancesPickingAtOnceNeverSendTheSameReminderTwice() throws Exception {
        for (int i = 0; i < 6; i++) {
            reminderFor("p" + i + "@example.com", i);
        }
        Clinic.clockAt(DUE);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<ReminderService.Report>> runs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            runs.add(pool.submit(() -> {
                go.await();
                return reminders.sendDue();
            }));
        }
        go.countDown();
        int sent = 0;
        for (Future<ReminderService.Report> run : runs) {
            sent += run.get().sent();
        }
        pool.shutdown();

        assertEquals(6, sent, "six reminders, sent by whoever picked them");
        assertEquals(6, Provider.deliveries());
        assertEquals(6, Provider.calls(), "one call each: no reminder was picked by two runs");
    }
}
