package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;

/**
 * What an operator can alert on. Every counter is read as a DELTA from a "before" reading: the registry outlives the tests and
 * other tests leave values behind (lab 24).
 */
@QuarkusTest
@QuarkusTestResource(NotificationStub.class)
@TestSecurity(user = "front-desk", roles = "reception")
class MetricsTest {

    @Inject
    TestData data;

    @Inject
    ReminderService reminders;

    long ana;
    long checkup;
    long bea;

    @BeforeEach
    void fixture() {
        data.reset();
        Provider.reset();
        CircuitBreakerMaintenance.get().resetAll();
        Clinic.clockAt(Clinic.NOW);
        ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        checkup = Clinic.treatment("Checkup", 30);
        bea = Clinic.patient("Bea", "bea@example.com");
    }

    private static double appointments(String outcome) {
        return Ops.sample(Ops.metrics(), "clinic_appointments_total", ".*outcome=\"" + outcome + "\".*");
    }

    private static double rejected(String code) {
        return Ops.sample(Ops.metrics(), "clinic_requests_rejected_total", ".*code=\"" + code + "\".*");
    }

    private static double reminderResult(String result) {
        return Ops.sample(Ops.metrics(), "clinic_reminders_total", ".*result=\"" + result + "\".*");
    }

    private static double gauge(String name) {
        return Ops.sample(Ops.metrics(), name, ".*");
    }

    @Test
    void theLifeOfAnAppointmentIsCounted() {
        double booked = appointments("booked");
        double rescheduled = appointments("rescheduled");
        double cancelled = appointments("cancelled");

        long id = Clinic.booked(ana, bea, checkup, "2026-03-09T09:00:00Z");
        Clinic.act(id, Clinic.etagOf(id), "reschedule", Map.of("startsAt", "2026-03-10T09:00:00Z")).then().statusCode(200);
        Clinic.act(id, Clinic.etagOf(id), "status", Map.of("to", "CANCELLED")).then().statusCode(200);

        assertEquals(booked + 1, appointments("booked"));
        assertEquals(rescheduled + 1, appointments("rescheduled"));
        assertEquals(cancelled + 1, appointments("cancelled"));
    }

    @Test
    void refusalsByTheRulesOfTheClinicAreCountedByCodeAndOtherErrorsAreNot() {
        Clinic.booked(ana, bea, checkup, "2026-03-09T09:00:00Z");
        long carl = Clinic.patient("Carl", "carl@example.com");
        double taken = rejected("SLOT_TAKEN");
        double outside = rejected("OUTSIDE_WORKING_HOURS");

        Clinic.book(ana, carl, checkup, "2026-03-09T09:00:00Z").then().statusCode(409);
        Clinic.book(ana, carl, checkup, "2026-03-09T07:00:00Z").then().statusCode(422);
        given().get("/api/appointments/999").then().statusCode(404);

        assertEquals(taken + 1, rejected("SLOT_TAKEN"));
        assertEquals(outside + 1, rejected("OUTSIDE_WORKING_HOURS"));
        assertFalse(Ops.metrics().contains("code=\"NOT_FOUND\""), "a 404 is not a rule of the clinic");
    }

    @Test
    void remindersAreCountedByResultAndTheProviderCallIsTimed() {
        double sent = reminderResult("sent");
        double failed = reminderResult("failed");
        double timedSent = Ops.sample(Ops.metrics(), "clinic_notifications_call_seconds_count", ".*outcome=\"sent\".*");
        Clinic.booked(ana, bea, checkup, "2026-03-02T10:00:00Z"); // due at once
        long nobody = Clinic.patient("Nobody", "nobody@invalid.test");
        Clinic.booked(ana, nobody, checkup, "2026-03-02T10:30:00Z");

        ReminderService.Report report = reminders.sendDue();

        assertEquals(1, report.sent());
        assertEquals(1, report.failed());
        assertEquals(sent + 1, reminderResult("sent"));
        assertEquals(failed + 1, reminderResult("failed"));
        assertEquals(timedSent + 1, Ops.sample(Ops.metrics(), "clinic_notifications_call_seconds_count", ".*outcome=\"sent\".*"));
    }

    @Test
    void theGaugesSayWhatNeedsAPerson() {
        Clinic.booked(ana, bea, checkup, "2026-03-09T09:00:00Z");
        long nobody = Clinic.patient("Nobody", "nobody@invalid.test");
        Clinic.booked(ana, nobody, checkup, "2026-03-02T10:30:00Z"); // due at once and refused for good
        assertEquals(2, gauge("clinic_reminders_pending"));
        assertEquals(0, gauge("clinic_reminders_failed"));

        reminders.sendDue();

        assertEquals(1, gauge("clinic_reminders_pending"), "the future one is still waiting");
        assertEquals(1, gauge("clinic_reminders_failed"), "the refused one waits for a person");
        assertEquals(0, gauge("clinic_reminders_overdue_seconds"), "nothing that is due is waiting");

        Clinic.clockAt("2026-03-12T12:00:00Z"); // the future one is now overdue (nobody ran the job)
        org.junit.jupiter.api.Assertions.assertTrue(gauge("clinic_reminders_overdue_seconds") > 0);
    }

    @Test
    void noTagCarriesAnIdAnEmailOrATime() {
        Clinic.booked(ana, bea, checkup, "2026-03-02T10:00:00Z");
        reminders.sendDue();

        String metrics = Ops.metrics();
        assertFalse(metrics.contains("bea@example.com"));
        assertFalse(metrics.contains("reminder-1"));
        assertFalse(metrics.matches("(?s).*clinic_[a-z_]+\\{[^}]*(id|email|startsAt)=.*"), "no tag named like an identifier");
    }
}
