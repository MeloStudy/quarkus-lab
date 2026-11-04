package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;

/**
 * Readiness decides who gets traffic. The reminders may be in trouble - the provider down, messages given up - and the clinic must
 * still be ready: booking an appointment does not need the notification provider.
 */
@QuarkusTest
@QuarkusTestResource(NotificationStub.class)
@TestSecurity(user = "front-desk", roles = "reception")
class HealthTest {

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
        CircuitBreakerMaintenance.get().resetAll();
        Clinic.clockAt(Clinic.NOW);
        ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        checkup = Clinic.treatment("Checkup", 30);
    }

    private void book(String email, int n) {
        long patient = Clinic.patient("Patient " + n, email);
        Clinic.booked(ana, patient, checkup, String.format("2026-03-09T%02d:%02d:00Z", 8 + n / 2, (n % 2) * 30));
    }

    @Test
    void aHealthyClinicIsLiveAndReadyAndTheReminderCheckExplainsItself() {
        Ops.get("/q/health/live").then().statusCode(200).body("status", equalTo("UP"));

        Ops.get("/q/health/ready").then().statusCode(200).body("status", equalTo("UP"))
                .body("checks.name", hasItem("reminders")).body("checks.find { it.name == 'reminders' }.data.circuit", equalTo("CLOSED"))
                .body("checks.find { it.name == 'reminders' }.data.failed", equalTo(0));
    }

    @Test
    void healthAndMetricsAreNotOnThePublicPort() {
        Ops.onPublicPort("/q/health/ready").then().statusCode(404);
        Ops.onPublicPort("/q/metrics").then().statusCode(404);
    }

    @Test
    void aProviderOutageDoesNotTakeTheClinicOutOfRotation() {
        for (int i = 0; i < 8; i++) {
            book("p" + i + "@example.com", i);
        }
        Clinic.clockAt("2026-03-08T12:00:00Z");
        Provider.failNext(1000, 503);
        ReminderService.Report report = reminders.sendDue();

        // the provider is down, the breaker opened, reminders are piling up ...
        Ops.get("/q/health/ready").then().statusCode(200).body("status", equalTo("UP"))
                .body("checks.find { it.name == 'reminders' }.data.circuit", equalTo("OPEN"))
                .body("checks.find { it.name == 'reminders' }.data.pending", equalTo(8))
                .body("checks.find { it.name == 'reminders' }.data.oldestOverdueSeconds", greaterThanOrEqualTo(0));
        org.junit.jupiter.api.Assertions.assertTrue(report.postponed() > 0, report.toString());
        // ... and the clinic still takes appointments
        long late = Clinic.patient("Late", "late@example.com");
        Clinic.book(ana, late, checkup, "2026-03-10T09:00:00Z").then().statusCode(201);
    }

    @Test
    void remindersThatGaveUpAreVisibleToTheOperatorAndStillNotAReasonToBeUnready() {
        book("nobody@invalid.test", 0);
        Clinic.clockAt("2026-03-08T12:00:00Z");
        reminders.sendDue();

        Ops.get("/q/health/ready").then().statusCode(200).body("status", equalTo("UP"))
                .body("checks.find { it.name == 'reminders' }.data.failed", equalTo(1));
        given().get("/api/reminders").then().body("[0].status", equalTo("FAILED"));
    }
}
