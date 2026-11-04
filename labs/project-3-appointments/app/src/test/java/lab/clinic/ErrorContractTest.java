package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Every failure, expected or not, leaves the API in the same shape, and says nothing about the inside. */
@QuarkusTest
class ErrorContractTest {

    @Inject
    TestData data;

    long ana;
    long checkup;
    long bea;

    @BeforeEach
    void fixture() {
        data.reset();
        Clinic.clockAt(Clinic.NOW);
        ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        checkup = Clinic.treatment("Checkup", 30);
        bea = Clinic.patient("Bea", "bea@example.com");
        Clinic.book(ana, bea, checkup, "2026-03-09T09:00:00Z").then().statusCode(201); // the valid request the cases below distort
    }

    private io.restassured.response.Response post(String body) {
        return given().contentType(ContentType.JSON).body(body).post("/api/appointments");
    }

    private String body(String startsAt) {
        return "{\"practitionerId\":" + ana + ",\"patientId\":" + bea + ",\"treatmentId\":" + checkup + ",\"startsAt\":\""
                + startsAt + "\"}";
    }

    @Test
    void aLocalTimeWithoutAnOffsetIsRejectedBecauseItMeansNothing() {
        post(body("2026-03-09T10:30:00")).then().statusCode(400).body("code", equalTo("MALFORMED_REQUEST"))
                .body("message", not(containsString("Instant"))).body("message", not(containsString("jackson")));
        post(body("2026-03-09T10:30:00+01:00")).then().statusCode(201); // the same minute with its offset is fine
    }

    @Test
    void syntacticallyBrokenJsonAndUnknownFieldsAreMalformedRequests() {
        post("{\"practitionerId\": ").then().statusCode(400).body("code", equalTo("MALFORMED_REQUEST"));
        post("{\"practitionerId\":" + ana + ",\"patientId\":" + bea + ",\"treatmentId\":" + checkup
                + ",\"startsAt\":\"2026-03-09T10:30:00Z\",\"practitionnerId\":1}").then().statusCode(400)
                .body("code", equalTo("MALFORMED_REQUEST"));
    }

    @Test
    void missingFieldsAreFieldErrors() {
        post("{}").then().statusCode(400).body("code", equalTo("VALIDATION_ERROR"))
                .body("errors.field", org.hamcrest.Matchers.hasItems("practitionerId", "patientId", "treatmentId", "startsAt"));
    }

    @Test
    void anUnknownRouteAndAWrongMethodUseTheSameShape() {
        given().get("/api/nothing").then().statusCode(404).body("code", equalTo("NOT_FOUND"));
        given().delete("/api/appointments/1").then().statusCode(405).body("code", equalTo("HTTP_405"));
        given().contentType(ContentType.XML).body("<a/>").post("/api/appointments").then().statusCode(415)
                .body("code", equalTo("HTTP_415"));
    }

    @Test
    void anUnexpectedFailureRevealsOnlyAnId() {
        given().get("/test-only/boom").then().statusCode(500).body("code", equalTo("INTERNAL_ERROR"))
                .body("errorId", notNullValue()).body("message", not(containsString("jdbc")))
                .body("message", not(containsString("secret")));
    }

    @Test
    void theRefusalOfADomainRuleHasTheSameShapeAsTheRest() {
        Clinic.book(ana, bea, checkup, "2026-03-09T09:00:00Z").then().statusCode(409).body("code", equalTo("SLOT_TAKEN"))
                .body("message", notNullValue()).body("errors", org.hamcrest.Matchers.nullValue());
        Map<String, Object> unknown = Map.of("practitionerId", 999, "patientId", bea, "treatmentId", checkup, "startsAt", "2026-03-09T09:00:00Z");
        given().contentType(ContentType.JSON).body(unknown).post("/api/appointments").then().statusCode(404).body("code", equalTo("NOT_FOUND"));
    }
}
