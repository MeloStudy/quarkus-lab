package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;

/**
 * Real tokens, real signatures, NO {@code @TestSecurity} (it is not authentication, and mixing both made a test depend on
 * which won: lab 20). Every refusal is paired with the allowed request next to it, so none passes for the wrong reason.
 *
 * <p>Fixture: Dr. Ana (1) and Dr. Ben (2); Bea (1) and Carl (2). Appointment 1 is Ana-Bea on Monday 2026-03-09 10:00 (Madrid);
 * appointment 2 is Ben-Carl on Tuesday 2026-03-10 10:00. "Now" is Monday 2026-03-02.
 */
@QuarkusTest
class SecurityTest {

    @Inject
    TestData data;

    @BeforeEach
    void fixture() {
        data.reset();
        Clinic.clockAt(Clinic.NOW);
        data.execute("INSERT INTO practitioner (name, created_at) VALUES ('Dr. Ana', now()), ('Dr. Ben', now())");
        data.execute("INSERT INTO patient (full_name, email, created_at) VALUES ('Bea', 'bea@example.com', now()), ('Carl', 'carl@example.com', now())");
        data.execute("INSERT INTO treatment (name, duration_minutes) VALUES ('Checkup', 30)");
        data.execute("INSERT INTO working_hours (practitioner_id, weekday, start_time, end_time) "
                + "SELECT p, d, '09:00', '13:00' FROM generate_series(1, 2) p, generate_series(1, 5) d");
        data.execute(appointment(1, 1, "2026-03-09 09:00:00+00"));
        data.execute(appointment(2, 2, "2026-03-10 09:00:00+00"));
        data.execute("INSERT INTO reminder (appointment_id, status, due_at, next_attempt_at, attempts) "
                + "SELECT id, 'PENDING', starts_at - interval '1 day', starts_at - interval '1 day', 0 FROM appointment");
    }

    private static String appointment(int practitioner, int patient, String start) {
        return "INSERT INTO appointment (practitioner_id, patient_id, treatment_id, starts_at, ends_at, status, version, created_at, updated_at) "
                + "VALUES (" + practitioner + ", " + patient + ", 1, '" + start + "', timestamptz '" + start + "' + interval '30 minutes', 'BOOKED', 0, now(), now())";
    }

    private static io.restassured.specification.RequestSpecification as(String token) {
        return given().auth().oauth2(token);
    }

    private static Response get(String token, String path) {
        return as(token).get(path);
    }

    private static Response book(String token, long patientId, String startsAt) {
        return as(token).contentType(ContentType.JSON)
                .body(Map.of("practitionerId", 1, "patientId", patientId, "treatmentId", 1, "startsAt", startsAt))
                .post("/api/appointments");
    }

    private static Response status(String token, long id, String to) {
        String etag = get(Tokens.reception(), "/api/appointments/" + id).then().extract().header("ETag");
        return as(token).header("If-Match", etag).contentType(ContentType.JSON).body(Map.of("to", to))
                .post("/api/appointments/" + id + "/status");
    }

    // ------------------------------------------------------------------------------------------ authentication

    @Test
    void noTokenIs401AndAValidOneIs200() {
        get(Tokens.reception(), "/api/practitioners").then().statusCode(200).body("$", hasSize(2));

        given().get("/api/practitioners").then().statusCode(401);
        given().contentType(ContentType.JSON).body(Map.of("name", "Dr. X")).post("/api/practitioners").then().statusCode(401);
    }

    @Test
    void aTokenThatCannotBeTrustedIs401WhateverItSays() {
        get(Tokens.reception(), "/api/practitioners").then().statusCode(200);

        get("garbage", "/api/practitioners").then().statusCode(401);
        get(Tokens.forged("reception"), "/api/practitioners").then().statusCode(401); // signed by somebody else
        String expired = Tokens.valid("front-desk", "reception").expiresAt(Instant.now().minus(Duration.ofMinutes(5))).sign();
        get(expired, "/api/practitioners").then().statusCode(401);
        String wrongIssuer = Tokens.valid("front-desk", "reception").issuer("https://evil.example/issuer").sign();
        get(wrongIssuer, "/api/practitioners").then().statusCode(401);
        String wrongAudience = Tokens.valid("front-desk", "reception").audience("another-api").sign();
        get(wrongAudience, "/api/practitioners").then().statusCode(401);
    }

    @Test
    void anEndpointNobodyAnnotatedIsClosedEvenToReception() {
        get(Tokens.reception(), "/api/practitioners").then().statusCode(200);

        get(Tokens.reception(), "/test-only/forgotten").then().statusCode(403);
    }

    // ------------------------------------------------------------------------------------------ roles

    @Test
    void onlyReceptionChangesTheReferenceData() {
        as(Tokens.reception()).contentType(ContentType.JSON).body(Map.of("name", "Dr. New")).post("/api/practitioners").then()
                .statusCode(201);

        for (String token : new String[] { Tokens.patient(1), Tokens.practitioner(1) }) {
            as(token).contentType(ContentType.JSON).body(Map.of("name", "Dr. Nope")).post("/api/practitioners").then().statusCode(403);
            as(token).contentType(ContentType.JSON).body(Map.of("name", "Nope", "durationMinutes", 30)).post("/api/treatments").then().statusCode(403);
            as(token).contentType(ContentType.JSON).body(Map.of("blocks", java.util.List.of())).put("/api/practitioners/1/working-hours").then().statusCode(403);
        }
        as(Tokens.patient(1)).contentType(ContentType.JSON).body(Map.of("fullName", "X", "email", "x@example.com")).post("/api/patients").then().statusCode(403);
    }

    @Test
    void everybodyAuthenticatedMayReadTheCatalogAndTheAvailability() {
        for (String token : new String[] { Tokens.reception(), Tokens.patient(1), Tokens.practitioner(1) }) {
            get(token, "/api/practitioners").then().statusCode(200);
            get(token, "/api/treatments").then().statusCode(200);
            as(token).queryParam("date", "2026-03-11").queryParam("treatmentId", 1).get("/api/practitioners/1/availability").then()
                    .statusCode(200).body("slots", hasSize(15));
        }
    }

    @Test
    void aPatientRecordIsVisibleToReceptionAndToItsOwnerOnly() {
        get(Tokens.reception(), "/api/patients/2").then().statusCode(200);
        get(Tokens.patient(2), "/api/patients/2").then().statusCode(200).body("fullName", equalTo("Carl"));

        get(Tokens.patient(1), "/api/patients/2").then().statusCode(404).body("code", equalTo("NOT_FOUND"));
        get(Tokens.practitioner(1), "/api/patients/2").then().statusCode(403);
    }

    // ------------------------------------------------------------------------------------------ ownership

    @Test
    void aPatientBooksForThemselvesAndForNobodyElse() {
        book(Tokens.patient(1), 1, "2026-03-11T09:00:00Z").then().statusCode(201);

        book(Tokens.patient(1), 2, "2026-03-12T09:00:00Z").then().statusCode(403).body("code", equalTo("FORBIDDEN"));
        // refused before anything is looked up: a patient that does not exist gives the same answer as one that does
        book(Tokens.patient(1), 999, "2026-03-12T09:00:00Z").then().statusCode(403);
        book(Tokens.practitioner(1), 1, "2026-03-12T09:00:00Z").then().statusCode(403); // not even a role that books
        book(Tokens.reception(), 2, "2026-03-12T09:00:00Z").then().statusCode(201); // reception books for anybody
    }

    @Test
    void anAppointmentOfSomebodyElseIsA404JustLikeOneThatDoesNotExist() {
        get(Tokens.patient(1), "/api/appointments/1").then().statusCode(200);
        get(Tokens.practitioner(1), "/api/appointments/1").then().statusCode(200);

        Response notMine = get(Tokens.patient(1), "/api/appointments/2");
        Response missing = get(Tokens.patient(1), "/api/appointments/999");
        notMine.then().statusCode(404);
        missing.then().statusCode(404);
        org.junit.jupiter.api.Assertions.assertEquals(missing.jsonPath().getString("code"), notMine.jsonPath().getString("code"));

        get(Tokens.practitioner(1), "/api/appointments/2").then().statusCode(404);
        get(Tokens.reception(), "/api/appointments/2").then().statusCode(200);
    }

    @Test
    void aListShowsOnlyWhatBelongsToTheCaller() {
        get(Tokens.reception(), "/api/appointments").then().statusCode(200).body("total", equalTo(2));
        get(Tokens.patient(1), "/api/appointments").then().statusCode(200).body("total", equalTo(1)).body("items.id", contains(1));
        get(Tokens.practitioner(2), "/api/appointments").then().statusCode(200).body("total", equalTo(1)).body("items.id", contains(2));

        // asking for somebody else is refused, not silently ignored
        as(Tokens.patient(1)).queryParam("patientId", 2).get("/api/appointments").then().statusCode(403);
        as(Tokens.practitioner(1)).queryParam("practitionerId", 2).get("/api/appointments").then().statusCode(403);
        as(Tokens.patient(1)).queryParam("patientId", 1).get("/api/appointments").then().statusCode(200).body("total", equalTo(1));
    }

    @Test
    void aPatientCancelsTheirOwnAppointmentButCannotJudgeAVisit() {
        status(Tokens.patient(2), 2, "CANCELLED").then().statusCode(200).body("status", equalTo("CANCELLED"));

        status(Tokens.patient(1), 1, "COMPLETED").then().statusCode(403).body("code", equalTo("FORBIDDEN"));
        status(Tokens.patient(1), 2, "CANCELLED").then().statusCode(404); // somebody else's
    }

    @Test
    void aPractitionerRecordsOnlyTheirOwnVisitsAndOnlyOnceTheyStarted() {
        Clinic.clockAt("2026-03-09T09:05:00Z"); // Ana's appointment started five minutes ago

        status(Tokens.practitioner(2), 1, "COMPLETED").then().statusCode(404); // Ben cannot even see Ana's
        status(Tokens.practitioner(1), 1, "COMPLETED").then().statusCode(200).body("status", equalTo("COMPLETED"));
    }

    @Test
    void aPatientReschedulesTheirOwnAppointmentOnly() {
        String etag = get(Tokens.reception(), "/api/appointments/1").then().extract().header("ETag");

        as(Tokens.patient(2)).header("If-Match", etag).contentType(ContentType.JSON).body(Map.of("startsAt", "2026-03-11T09:00:00Z"))
                .post("/api/appointments/1/reschedule").then().statusCode(404);
        as(Tokens.practitioner(1)).header("If-Match", etag).contentType(ContentType.JSON).body(Map.of("startsAt", "2026-03-11T09:00:00Z"))
                .post("/api/appointments/1/reschedule").then().statusCode(403);
        as(Tokens.patient(1)).header("If-Match", etag).contentType(ContentType.JSON).body(Map.of("startsAt", "2026-03-11T09:00:00Z"))
                .post("/api/appointments/1/reschedule").then().statusCode(200).body("startsAt", equalTo("2026-03-11T09:00:00Z"));
    }

    @Test
    void theRemindersAreForTheReceptionOnly() {
        get(Tokens.reception(), "/api/reminders").then().statusCode(200).body("$", hasSize(2));

        get(Tokens.patient(1), "/api/reminders").then().statusCode(403);
        get(Tokens.practitioner(1), "/api/reminders").then().statusCode(403);
        as(Tokens.patient(1)).post("/api/reminders/1/retry").then().statusCode(403);
        given().get("/api/reminders").then().statusCode(401);
    }

    @Test
    void aTokenWithARoleButWithoutItsRecordSeesAndChangesNothing() {
        String orphan = Tokens.valid("ghost", "patient").sign(); // no patient_id claim
        get(Tokens.patient(1), "/api/appointments").then().statusCode(200).body("total", equalTo(1));

        get(orphan, "/api/appointments").then().statusCode(200).body("total", equalTo(0));
        get(orphan, "/api/appointments/1").then().statusCode(404);
        book(orphan, 1, "2026-03-11T09:00:00Z").then().statusCode(403);
    }

    @Test
    void aClaimOfTheWrongTypeIsAnOrphanToo() {
        String text = Jwt.issuer(Tokens.ISSUER).audience(Tokens.AUDIENCE).subject("x").groups("patient").claim("patient_id", "1")
                .expiresIn(Duration.ofHours(1)).sign();
        get(Tokens.patient(1), "/api/appointments/1").then().statusCode(200);

        get(text, "/api/appointments/1").then().statusCode(404);
    }
}
