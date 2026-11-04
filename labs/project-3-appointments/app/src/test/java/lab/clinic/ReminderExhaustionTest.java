package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;

/**
 * A provider that stays down: the wait doubles with every failure and, after the fifth, the reminder stops and waits for a person.
 * The breaker is made irrelevant here (a huge window) so that this test is about the backoff and not about the breaker.
 */
@QuarkusTest
@QuarkusTestResource(NotificationStub.class)
@TestProfile(ReminderExhaustionTest.NoBreaker.class)
@TestSecurity(user = "front-desk", roles = "reception")
class ReminderExhaustionTest {

    public static class NoBreaker implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.fault-tolerance.\"lab.clinic.ReminderGateway/send\".circuit-breaker.request-volume-threshold", "1000");
        }
    }

    @Inject
    TestData data;

    @Inject
    ReminderService reminders;

    @BeforeEach
    void fixture() {
        data.reset();
        Provider.reset();
        CircuitBreakerMaintenance.get().resetAll();
        Clinic.clockAt(Clinic.NOW);
        long ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        long checkup = Clinic.treatment("Checkup", 30);
        long bea = Clinic.patient("Bea", "bea@example.com");
        Clinic.booked(ana, bea, checkup, "2026-03-02T10:00:00Z"); // within a day: due at once
    }

    private Instant nextAttempt() {
        return Instant.parse(given().get("/api/reminders").then().extract().path("[0].nextAttemptAt"));
    }

    @Test
    void theWaitDoublesAfterEveryFailureAndTheFifthOneGivesUp() {
        Provider.failNext(10_000, 503);
        Instant now = Instant.parse(Clinic.NOW);
        long[] expectedWaitSeconds = { 60, 120, 240, 480 };

        for (int attempt = 1; attempt <= 4; attempt++) {
            Clinic.clockAt(now.toString());
            ReminderService.Report report = reminders.sendDue();
            assertEquals(1, report.retrying(), "attempt " + attempt);
            assertEquals(Duration.ofSeconds(expectedWaitSeconds[attempt - 1]), Duration.between(now, nextAttempt()), "wait after attempt " + attempt);
            given().get("/api/reminders").then().body("[0].attempts", equalTo(attempt)).body("[0].status", equalTo("PENDING"));

            // before the wait is over nothing is picked: the application does not hammer a provider that is down
            Clinic.clockAt(now.plusSeconds(expectedWaitSeconds[attempt - 1] - 1).toString());
            assertEquals(0, reminders.sendDue().claimed(), "still waiting after attempt " + attempt);

            now = now.plusSeconds(expectedWaitSeconds[attempt - 1] + 1);
        }

        Clinic.clockAt(now.toString());
        assertEquals(1, reminders.sendDue().retrying());
        given().get("/api/reminders").then().body("[0].status", equalTo("FAILED")).body("[0].attempts", equalTo(5))
                .body("[0].lastError", containsString("gave up after 5 attempts"));

        // and it is not picked again, however much time passes
        Clinic.clockAt(now.plus(Duration.ofDays(2)).toString());
        assertEquals(0, reminders.sendDue().claimed());
        assertEquals(0, Provider.deliveries());
    }
}
