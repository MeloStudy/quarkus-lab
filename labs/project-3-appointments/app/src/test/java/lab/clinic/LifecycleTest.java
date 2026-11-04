package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

/** Reschedule, cancel, complete: the state machine, the ETag and the clock. */
@QuarkusTest
@TestSecurity(user = "front-desk", roles = "reception")
class LifecycleTest {

    private static final String MONDAY_10 = "2026-03-09T09:00:00Z"; // 10:00 in Madrid

    @Inject
    TestData data;

    long ana;
    long checkup;
    long bea;
    long carl;
    long id;

    @BeforeEach
    void fixture() {
        data.reset();
        Clinic.clockAt(Clinic.NOW);
        ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        checkup = Clinic.treatment("Checkup", 30);
        bea = Clinic.patient("Bea", "bea@example.com");
        carl = Clinic.patient("Carl", "carl@example.com");
        id = Clinic.booked(ana, bea, checkup, MONDAY_10);
    }

    private static Map<String, String> to(String status) {
        return Map.of("to", status);
    }

    @Test
    void cancellingFreesTheTimeForSomeoneElse() {
        Clinic.book(ana, carl, checkup, MONDAY_10).then().statusCode(409).body("code", equalTo("SLOT_TAKEN"));

        Clinic.act(id, Clinic.etagOf(id), "status", to("CANCELLED")).then().statusCode(200).body("status", equalTo("CANCELLED"))
                .body("version", equalTo(1)).header("ETag", equalTo("\"1\""));

        Clinic.book(ana, carl, checkup, MONDAY_10).then().statusCode(201);
    }

    @Test
    void aChangeNeedsIfMatchAndTheRightVersion() {
        String stale = Clinic.etagOf(id);
        Clinic.act(id, stale, "status", to("CANCELLED")).then().statusCode(200);

        Clinic.act(id, null, "status", to("COMPLETED")).then().statusCode(428).body("code", equalTo("PRECONDITION_REQUIRED"));
        Clinic.act(id, stale, "reschedule", Map.of("startsAt", "2026-03-10T09:00:00Z")).then().statusCode(412)
                .body("code", equalTo("PRECONDITION_FAILED"));
        Clinic.act(id, "garbage", "status", to("COMPLETED")).then().statusCode(412);
        Clinic.act(id, "*", "status", to("COMPLETED")).then().statusCode(412);
    }

    @Test
    void rescheduleMovesTheAppointmentAndFreesTheOldTime() {
        Clinic.act(id, Clinic.etagOf(id), "reschedule", Map.of("startsAt", "2026-03-10T09:00:00Z")).then().statusCode(200)
                .body("local", equalTo("2026-03-10T10:00:00+01:00")).body("endsAt", equalTo("2026-03-10T09:30:00Z"))
                .body("version", equalTo(1));

        Clinic.book(ana, carl, checkup, MONDAY_10).then().statusCode(201); // the old time is free
        Clinic.book(ana, carl, checkup, "2026-03-10T09:00:00Z").then().statusCode(409); // the new one is not
    }

    @Test
    void rescheduleFollowsTheSameRulesAsBooking() {
        String etag = Clinic.etagOf(id);

        Clinic.act(id, etag, "reschedule", Map.of("startsAt", "2026-03-09T09:10:00Z")).then().statusCode(422)
                .body("code", equalTo("INVALID_START"));
        Clinic.act(id, etag, "reschedule", Map.of("startsAt", "2026-03-14T09:00:00Z")).then().statusCode(422)
                .body("code", equalTo("OUTSIDE_WORKING_HOURS"));

        long other = Clinic.booked(ana, carl, checkup, "2026-03-10T09:00:00Z");
        Clinic.act(id, etag, "reschedule", Map.of("startsAt", "2026-03-10T09:00:00Z")).then().statusCode(409)
                .body("code", equalTo("SLOT_TAKEN"));
        given().get("/api/appointments/" + id).then().body("startsAt", equalTo(MONDAY_10)).body("version", equalTo(0));
        given().get("/api/appointments/" + other).then().statusCode(200);
    }

    @Test
    void rescheduleOntoItsOwnTimeIsNotAConflictWithItself() {
        Clinic.act(id, Clinic.etagOf(id), "reschedule", Map.of("startsAt", "2026-03-09T09:15:00Z")).then().statusCode(200)
                .body("startsAt", equalTo("2026-03-09T09:15:00Z")); // overlaps its own old interval: still fine
    }

    @Test
    void nothingLeavesACancelledAppointment() {
        Clinic.act(id, Clinic.etagOf(id), "status", to("CANCELLED")).then().statusCode(200);

        Clinic.act(id, Clinic.etagOf(id), "status", to("CANCELLED")).then().statusCode(409).body("code", equalTo("INVALID_TRANSITION"));
        Clinic.act(id, Clinic.etagOf(id), "reschedule", Map.of("startsAt", "2026-03-10T09:00:00Z")).then().statusCode(409)
                .body("code", equalTo("INVALID_TRANSITION"));
        Clinic.act(id, Clinic.etagOf(id), "status", to("BOOKED")).then().statusCode(409);
    }

    @Test
    void anAppointmentCanBeCompletedOrMarkedNoShowOnlyOnceItHasStarted() {
        Clinic.act(id, Clinic.etagOf(id), "status", to("COMPLETED")).then().statusCode(409).body("code", equalTo("TOO_EARLY"));

        Clinic.clockAt("2026-03-09T09:05:00Z"); // five minutes after the start
        Clinic.act(id, Clinic.etagOf(id), "status", to("NO_SHOW")).then().statusCode(200).body("status", equalTo("NO_SHOW"));
    }

    @Test
    void anAppointmentThatHasStartedCannotBeCancelledOrMoved() {
        Clinic.clockAt("2026-03-09T09:05:00Z");

        Clinic.act(id, Clinic.etagOf(id), "status", to("CANCELLED")).then().statusCode(409).body("code", equalTo("ALREADY_STARTED"));
        Clinic.act(id, Clinic.etagOf(id), "reschedule", Map.of("startsAt", "2026-03-10T09:00:00Z")).then().statusCode(409)
                .body("code", equalTo("ALREADY_STARTED"));
    }

    @Test
    void anUnknownAppointmentOrStatusIsARejection() {
        Clinic.act(999, "\"0\"", "status", to("CANCELLED")).then().statusCode(404);
        Clinic.act(id, Clinic.etagOf(id), "status", to("PENDING")).then().statusCode(400).body("code", equalTo("MALFORMED_REQUEST"));
        given().get("/api/appointments/999").then().statusCode(404).body("code", equalTo("NOT_FOUND"));
    }
}
