package lab.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
class ProjectApiTest {

    @Inject
    TestData data;

    @BeforeEach
    void cleanDatabase() {
        data.reset();
    }

    @Test
    void createsAProjectAndTellsWhereItLives() {
        Tracker.project("TRK").then().statusCode(201)
                .header("Location", endsWith("/api/projects/TRK"))
                .body("code", equalTo("TRK"), "name", equalTo("Project TRK"), "createdAt", notNullValue());

        given().get("/api/projects/TRK").then().statusCode(200).body("code", equalTo("TRK"));
    }

    @Test
    void theCodeMustBeTwoToFiveCapitalLetters() {
        Tracker.project("TRK").then().statusCode(201); // the baseline is valid: the 400s below are about the code

        for (String bad : new String[] { "T", "trk", "TOOLONG", "TR1", "T-K" }) {
            Tracker.project(bad).then().statusCode(400)
                    .body("code", equalTo("VALIDATION_ERROR"), "errors.field", hasItem("code"));
        }
    }

    @Test
    void theNameIsRequiredAndBounded() {
        given().contentType(ContentType.JSON).body("{\"code\":\"TRK\",\"name\":\" \"}").post("/api/projects")
                .then().statusCode(400).body("errors.field", hasItem("name"));
        given().contentType(ContentType.JSON).body("{\"code\":\"TRK\",\"name\":\"" + "x".repeat(81) + "\"}")
                .post("/api/projects").then().statusCode(400).body("errors.field", hasItem("name"));
    }

    @Test
    void aCodeCanBeUsedOnlyOnce() {
        Tracker.project("TRK").then().statusCode(201);

        Tracker.project("TRK").then().statusCode(409).body("code", equalTo("DUPLICATE_PROJECT"));
        // the loser did not leave anything behind
        given().get("/api/projects").then().body("code", contains("TRK"));
    }

    @Test
    void anUnknownProjectIs404WithTheErrorShape() {
        given().get("/api/projects/NOPE").then().statusCode(404).body("code", equalTo("NOT_FOUND"));
        given().get("/api/projects/NOPE/summary").then().statusCode(404).body("code", equalTo("NOT_FOUND"));
    }

    @Test
    void projectsAreListedByCode() {
        Tracker.project("ZED").then().statusCode(201);
        Tracker.project("ABC").then().statusCode(201);

        given().get("/api/projects").then().statusCode(200).body("code", contains("ABC", "ZED"));
    }
}
