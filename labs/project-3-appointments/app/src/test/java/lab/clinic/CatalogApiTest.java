package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
class CatalogApiTest {

    @Inject
    TestData data;

    @BeforeEach
    void clean() {
        data.reset();
    }

    @Test
    void aPractitionerIsCreatedAndFound() {
        given().contentType(ContentType.JSON).body(Map.of("name", "  Dr. Ana  ")).post("/api/practitioners").then()
                .statusCode(201).header("Location", endsWith("/api/practitioners/1")).body("name", equalTo("Dr. Ana"));

        given().get("/api/practitioners/1").then().statusCode(200).body("name", equalTo("Dr. Ana"));
        given().get("/api/practitioners/999").then().statusCode(404).body("code", equalTo("NOT_FOUND"));
    }

    @Test
    void aBlankNameIsAFieldError() {
        given().contentType(ContentType.JSON).body(Map.of("name", " ")).post("/api/practitioners").then().statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR")).body("errors.field", contains("name"));
    }

    @Test
    void aTreatmentIsAWholeNumberOfQuarterHours() {
        Clinic.treatment("Checkup", 30);

        given().contentType(ContentType.JSON).body(Map.of("name", "Odd", "durationMinutes", 20)).post("/api/treatments")
                .then().statusCode(422).body("code", equalTo("INVALID_DURATION"));
        given().contentType(ContentType.JSON).body(Map.of("name", "Marathon", "durationMinutes", 600)).post("/api/treatments")
                .then().statusCode(400).body("errors.field", contains("durationMinutes"));
    }

    @Test
    void aTreatmentNameIsUnique() {
        Clinic.treatment("Checkup", 30);

        given().contentType(ContentType.JSON).body(Map.of("name", "Checkup", "durationMinutes", 45)).post("/api/treatments")
                .then().statusCode(409).body("code", equalTo("TREATMENT_EXISTS"));
    }

    @Test
    void anEmailIsTakenWhateverItsCase() {
        Clinic.patient("Bea", "bea@example.com");

        given().contentType(ContentType.JSON).body(Map.of("fullName", "Other", "email", "BEA@example.com"))
                .post("/api/patients").then().statusCode(409).body("code", equalTo("EMAIL_TAKEN"));
        given().contentType(ContentType.JSON).body(Map.of("fullName", "Bad", "email", "not-an-email")).post("/api/patients")
                .then().statusCode(400).body("errors.field", contains("email"));
    }

    @Test
    void workingHoursAreReplacedAsAWholeAndListedInOrder() {
        long ana = Clinic.practitioner("Dr. Ana");
        Clinic.hours(ana, Clinic.block(2, "09:00", "13:00"), Clinic.block(1, "15:00", "18:00"), Clinic.block(1, "09:00", "13:00"));

        given().get("/api/practitioners/" + ana + "/working-hours").then().statusCode(200).body("$", hasSize(3))
                .body("weekday", contains(1, 1, 2)).body("start", contains("09:00:00", "15:00:00", "09:00:00"));

        // a second PUT does not add to the first one: it replaces it
        Clinic.hours(ana, Clinic.block(5, "10:00", "12:00"));
        given().get("/api/practitioners/" + ana + "/working-hours").then().body("$", hasSize(1)).body("weekday", contains(5));
    }

    @Test
    void twoBlocksOfOneDayThatOverlapAreRefusedAndTheOldWeekSurvives() {
        long ana = Clinic.practitioner("Dr. Ana");
        Clinic.hours(ana, Clinic.block(1, "09:00", "13:00"));

        given().contentType(ContentType.JSON)
                .body(Map.of("blocks", List.of(Clinic.block(3, "09:00", "13:00"), Clinic.block(3, "12:00", "14:00"))))
                .put("/api/practitioners/" + ana + "/working-hours").then().statusCode(422)
                .body("code", equalTo("OVERLAPPING_HOURS"));

        // the failed replacement rolled back as a whole: Monday is still there
        given().get("/api/practitioners/" + ana + "/working-hours").then().body("weekday", contains(1));
    }

    @Test
    void aBlockMustStartBeforeItEndsAndOnARealWeekday() {
        long ana = Clinic.practitioner("Dr. Ana");

        given().contentType(ContentType.JSON).body(Map.of("blocks", List.of(Clinic.block(1, "13:00", "09:00"))))
                .put("/api/practitioners/" + ana + "/working-hours").then().statusCode(422)
                .body("code", equalTo("INVALID_HOURS"));
        given().contentType(ContentType.JSON).body(Map.of("blocks", List.of(Clinic.block(8, "09:00", "13:00"))))
                .put("/api/practitioners/" + ana + "/working-hours").then().statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
        given().contentType(ContentType.JSON).body(Map.of("blocks", List.of(Clinic.block(1, "09:00", "13:00"))))
                .put("/api/practitioners/999/working-hours").then().statusCode(404);
    }
}
