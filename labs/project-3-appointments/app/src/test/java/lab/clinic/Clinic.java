package lab.clinic;

import static io.restassured.RestAssured.given;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import io.quarkus.test.junit.QuarkusMock;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 * Test-only: the clinic as the tests see it. The fixture week: "now" is Monday 2026-03-02 09:00 in Madrid (08:00Z, the
 * clocks are still on winter time); Dr. Ana works Monday to Friday 09:00-13:00 and 15:00-18:00 (a lunch break).
 */
final class Clinic {

    /** A Monday morning in winter time. The clocks go forward on Sunday 2026-03-29 and back on Sunday 2026-10-25. */
    static final String NOW = "2026-03-02T08:00:00Z";

    private Clinic() {
    }

    /** Replaces the clock of the application for the current test (lab 16). */
    static void clockAt(String instant) {
        QuarkusMock.installMockForType(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC), Clock.class);
    }

    static long create(String path, Map<String, ?> body) {
        return given().contentType(ContentType.JSON).body(body).post(path).then().statusCode(201).extract().jsonPath()
                .getLong("id");
    }

    static long practitioner(String name) {
        return create("/api/practitioners", Map.of("name", name));
    }

    static long treatment(String name, int minutes) {
        return create("/api/treatments", Map.of("name", name, "durationMinutes", minutes));
    }

    static long patient(String name, String email) {
        return create("/api/patients", Map.of("fullName", name, "email", email));
    }

    static void hours(long practitionerId, Object... blocks) {
        given().contentType(ContentType.JSON).body(Map.of("blocks", java.util.List.of(blocks)))
                .put("/api/practitioners/" + practitionerId + "/working-hours").then().statusCode(200);
    }

    static Map<String, Object> block(int weekday, String start, String end) {
        return Map.of("weekday", weekday, "start", start, "end", end);
    }

    /** Monday to Friday, 09:00-13:00 and 15:00-18:00. */
    static void weekdayHours(long practitionerId) {
        Object[] blocks = new Object[10];
        for (int d = 1; d <= 5; d++) {
            blocks[(d - 1) * 2] = block(d, "09:00", "13:00");
            blocks[(d - 1) * 2 + 1] = block(d, "15:00", "18:00");
        }
        hours(practitionerId, blocks);
    }

    static Response book(long practitionerId, long patientId, long treatmentId, String startsAt) {
        return given().contentType(ContentType.JSON).body(Map.of("practitionerId", practitionerId, "patientId", patientId,
                "treatmentId", treatmentId, "startsAt", startsAt)).post("/api/appointments");
    }

    /** Books and insists on success; returns the id. */
    static long booked(long practitionerId, long patientId, long treatmentId, String startsAt) {
        return book(practitionerId, patientId, treatmentId, startsAt).then().statusCode(201).extract().jsonPath()
                .getLong("id");
    }

    static String etagOf(long appointmentId) {
        return given().get("/api/appointments/" + appointmentId).then().statusCode(200).extract().header("ETag");
    }

    static Response act(long appointmentId, String etag, String action, Map<String, ?> body) {
        var request = given().contentType(ContentType.JSON).body(body);
        if (etag != null) {
            request = request.header("If-Match", etag);
        }
        return request.post("/api/appointments/" + appointmentId + "/" + action);
    }
}
