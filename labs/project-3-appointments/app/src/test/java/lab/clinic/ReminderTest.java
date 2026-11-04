package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

/**
 * The life of a reminder, with the fake provider counting what really went out. "Now" is Monday 2026-03-02 09:00 in Madrid; the
 * reminders are due 24 hours before the appointment. The job is called by hand, so time only passes when the test says so.
 */
@QuarkusTest
@QuarkusTestResource(NotificationStub.class)
@TestSecurity(user = "front-desk", roles = "reception")
class ReminderTest {

    private static final String MONDAY_10 = "2026-03-09T09:00:00Z"; // 10:00 in Madrid
    private static final String DUE_AT = "2026-03-08T09:00:00Z";

    @Inject
    TestData data;

    @Inject
    ReminderService reminders;

    @Inject
    ReminderStore store;

    long ana;
    long checkup;
    long bea;

    @BeforeEach
    void fixture() {
        data.reset();
        Provider.reset();
        Clinic.clockAt(Clinic.NOW);
        ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        checkup = Clinic.treatment("Checkup", 30);
        bea = Clinic.patient("Bea", "bea@example.com");
    }

    private static io.restassured.response.ValidatableResponse reminderList() {
        return given().get("/api/reminders").then().statusCode(200);
    }

    @Test
    void bookingCreatesAReminderDueOneDayBeforeInTheSameTransaction() {
        long id = Clinic.booked(ana, bea, checkup, MONDAY_10);

        reminderList().body("$", hasSize(1)).body("[0].appointmentId", equalTo((int) id)).body("[0].status", equalTo("PENDING"))
                .body("[0].dueAt", equalTo(DUE_AT)).body("[0].attempts", equalTo(0));
    }

    @Test
    void aRefusedBookingLeavesNoReminderBehind() {
        Clinic.booked(ana, bea, checkup, MONDAY_10);
        long carl = Clinic.patient("Carl", "carl@example.com");

        Clinic.book(ana, carl, checkup, MONDAY_10).then().statusCode(409);

        reminderList().body("$", hasSize(1));
    }

    @Test
    void anAppointmentCloserThanADayIsRemindedAtOnce() {
        Clinic.booked(ana, bea, checkup, "2026-03-02T10:00:00Z"); // today, 11:00 in Madrid

        reminderList().body("[0].dueAt", equalTo(Clinic.NOW));
        assertEquals(1, reminders.sendDue().sent());
    }

    @Test
    void nothingIsSentBeforeItIsDueAndOnceItIsDueItGoesOutExactlyOnce() {
        Clinic.booked(ana, bea, checkup, MONDAY_10);

        assertEquals(new ReminderService.Report(0, 0, 0, 0, 0), reminders.sendDue(), "not due yet");
        assertEquals(0, Provider.calls());

        Clinic.clockAt("2026-03-08T09:00:01Z");
        assertEquals(new ReminderService.Report(1, 1, 0, 0, 0), reminders.sendDue());
        assertEquals(1, Provider.deliveries());
        assertEquals(java.util.List.of("reminder-1-v0 -> bea@example.com"), Provider.seen());
        reminderList().body("[0].status", equalTo("SENT")).body("[0].attempts", equalTo(1));

        // a second tick finds nothing to do: SENT is not picked again
        assertEquals(0, reminders.sendDue().claimed());
        assertEquals(1, Provider.deliveries());
        assertEquals(1, Provider.calls());
    }

    @Test
    void cancellingTheAppointmentCancelsAReminderThatHasNotGoneOut() {
        long id = Clinic.booked(ana, bea, checkup, MONDAY_10);
        Clinic.act(id, Clinic.etagOf(id), "status", Map.of("to", "CANCELLED")).then().statusCode(200);

        reminderList().body("[0].status", equalTo("CANCELLED"));
        Clinic.clockAt("2026-03-08T10:00:00Z");
        assertEquals(0, reminders.sendDue().claimed());
        assertEquals(0, Provider.calls());
    }

    @Test
    void aCancelledAppointmentWhoseReminderWasAlreadyPickedIsNotSent() {
        long id = Clinic.booked(ana, bea, checkup, MONDAY_10);
        Clinic.clockAt("2026-03-08T10:00:00Z");
        long reminder = store.claim().get(0);

        Clinic.act(id, Clinic.etagOf(id), "status", Map.of("to", "CANCELLED")).then().statusCode(200);

        assertEquals(java.util.Optional.empty(), store.prepare(reminder), "the appointment is not booked any more");
        assertEquals(0, Provider.calls());
    }

    @Test
    void aSentReminderStaysAsHistoryWhenTheAppointmentIsCancelled() {
        long id = Clinic.booked(ana, bea, checkup, MONDAY_10);
        Clinic.clockAt("2026-03-08T10:00:00Z");
        reminders.sendDue();

        Clinic.act(id, Clinic.etagOf(id), "status", Map.of("to", "CANCELLED")).then().statusCode(200);

        reminderList().body("[0].status", equalTo("SENT"));
    }

    @Test
    void movingTheAppointmentTellsThePatientTheNewTimeWithANewIdempotencyKey() {
        long id = Clinic.booked(ana, bea, checkup, MONDAY_10);
        Clinic.clockAt("2026-03-08T10:00:00Z");
        reminders.sendDue();
        assertEquals(1, Provider.deliveries());

        Clinic.act(id, Clinic.etagOf(id), "reschedule", Map.of("startsAt", "2026-03-10T09:00:00Z")).then().statusCode(200);
        reminderList().body("[0].status", equalTo("PENDING")).body("[0].dueAt", equalTo("2026-03-09T09:00:00Z"))
                .body("[0].attempts", equalTo(0));

        Clinic.clockAt("2026-03-09T09:30:00Z");
        assertEquals(1, reminders.sendDue().sent());
        // a different key (it says which version of the appointment it describes): the provider does not mistake it for the first
        assertEquals(2, Provider.deliveries());
        assertEquals(java.util.List.of("reminder-1-v0 -> bea@example.com", "reminder-1-v1 -> bea@example.com"), Provider.seen());
    }

    @Test
    void anAnswerForAnAppointmentThatMovedInTheMeantimeIsDropped() {
        long id = Clinic.booked(ana, bea, checkup, MONDAY_10);
        Clinic.clockAt("2026-03-08T10:00:00Z");
        long reminder = store.claim().get(0);
        ReminderStore.Outgoing outgoing = store.prepare(reminder).orElseThrow(); // "sending" version 0 ...

        Clinic.act(id, Clinic.etagOf(id), "reschedule", Map.of("startsAt", "2026-03-10T09:00:00Z")).then().statusCode(200); // ... it moves ...
        store.sent(outgoing); // ... and the provider answers for the old time

        reminderList().body("[0].status", equalTo("PENDING")); // the new time still has to be told
    }

    @Test
    void theApiOfRemindersIsForTheReceptionOnlyAndFiltersByStatus() {
        Clinic.booked(ana, bea, checkup, MONDAY_10);

        given().queryParam("status", "SENT").get("/api/reminders").then().statusCode(200).body("$", hasSize(0));
        given().queryParam("status", "PENDING").get("/api/reminders").then().statusCode(200).body("$", hasSize(1));
        given().queryParam("status", "LOST").get("/api/reminders").then().statusCode(400);
        given().post("/api/reminders/999/retry").then().statusCode(404);
    }
}
