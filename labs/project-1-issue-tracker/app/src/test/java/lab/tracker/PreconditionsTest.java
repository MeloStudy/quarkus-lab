package lab.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** ETag / If-Match: the version of an issue is the proof that the client saw its latest state. */
@QuarkusTest
class PreconditionsTest {

    @Inject
    TestData data;

    String key;

    @BeforeEach
    void anIssue() {
        data.reset();
        Tracker.project("TRK").then().statusCode(201);
        key = Tracker.issue("TRK", "Original");
    }

    @Test
    void theEtagIsTheVersionAndAnUpToDateClientGets304() {
        String etag = Tracker.etag(key);
        assertEquals("\"0\"", etag);

        given().header("If-None-Match", etag).get("/api/issues/{key}", key).then().statusCode(304)
                .body(emptyOrNullString());
        given().header("If-None-Match", "\"7\"").get("/api/issues/{key}", key).then().statusCode(200);
    }

    @Test
    void aChangeNeedsIfMatch() {
        Tracker.put(key, null, Tracker.updateBody("Edited", "HIGH", null)).then().statusCode(428)
                .body("code", equalTo("PRECONDITION_REQUIRED"));
        Tracker.transition(key, null, "IN_PROGRESS").then().statusCode(428);

        Tracker.get(key).then().body("title", equalTo("Original"), "version", equalTo(0));
    }

    @Test
    void aStaleClientIsToldToReloadAndNothingChanges() {
        String stale = Tracker.etag(key);
        Tracker.put(key, stale, Tracker.updateBody("Edited by somebody else", "HIGH", null)).then().statusCode(200);

        Tracker.put(key, stale, Tracker.updateBody("Edited by me", "LOW", null)).then().statusCode(412)
                .body("code", equalTo("PRECONDITION_FAILED"));
        Tracker.transition(key, stale, "IN_PROGRESS").then().statusCode(412);

        Tracker.get(key).then().body("title", equalTo("Edited by somebody else"), "priority", equalTo("HIGH"));
    }

    @Test
    void valuesThatAreNotOurEtagsNeverMatch() {
        for (String garbage : new String[] { "*", "abc", "W/\"0\"", "\"\"", "0 0" }) {
            Tracker.put(key, garbage, Tracker.updateBody("Edited", "HIGH", null)).then().statusCode(412);
        }
        Tracker.get(key).then().body("title", equalTo("Original"));
    }

    @Test
    void everyChangeGivesANewEtag() {
        String v0 = Tracker.etag(key);
        String v1 = Tracker.put(key, v0, Tracker.updateBody("Edited", "HIGH", null)).then().statusCode(200)
                .header("ETag", equalTo("\"1\"")).extract().header("ETag");
        Tracker.assign(key, "alice");
        String v2 = Tracker.etag(key);
        String v3 = Tracker.transition(key, v2, "IN_PROGRESS").then().statusCode(200).extract().header("ETag");

        assertNotEquals(v0, v1);
        assertNotEquals(v2, v3);
        assertEquals(v3, Tracker.etag(key));
    }

    @Test
    void sendingTheSameValuesAgainChangesNothing() {
        Tracker.open("TRK", "{\"title\":\"Labelled\",\"type\":\"BUG\",\"labels\":[\"a\",\"b\"]}").then().statusCode(201);
        String labelled = "TRK-2";
        Tracker.assign(labelled, "alice"); // version 1
        String etag = Tracker.etag(labelled);
        var before = Tracker.get(labelled).jsonPath();

        Tracker.put(labelled, etag, "{\"title\":\"Labelled\",\"priority\":\"MEDIUM\",\"assignee\":\"alice\","
                + "\"labels\":[\"b\",\"a\"]}").then().statusCode(200).header("ETag", equalTo(etag));

        var after = Tracker.get(labelled).jsonPath();
        assertEquals(before.getString("updatedAt"), after.getString("updatedAt"));
        given().get("/api/issues/{key}/history", labelled).then()
                .body("kind", contains("CREATED", "ASSIGNEE_CHANGED"));
    }

    @Test
    void aCommentDoesNotChangeTheIssueSoTheEtagSurvives() {
        String etag = Tracker.etag(key);

        given().contentType(ContentType.JSON).body("{\"author\":\"bob\",\"body\":\"I can reproduce it\"}")
                .post("/api/issues/{key}/comments", key).then().statusCode(201);

        assertEquals(etag, Tracker.etag(key));
    }

    @Test
    void putReplacesTheEditablePartSoWhatIsAbsentIsCleared() {
        Tracker.open("TRK", "{\"title\":\"Full\",\"type\":\"TASK\",\"description\":\"details\",\"labels\":[\"a\",\"b\"]}")
                .then().statusCode(201);
        String full = "TRK-2";

        Tracker.put(full, Tracker.etag(full), Tracker.updateBody("Full", "MEDIUM", null)).then().statusCode(200)
                .body("description", nullValue(), "labels", empty(), "assignee", nullValue());
    }

    @Test
    void theOrderOfChecksIsExistsThenVersionThenState() {
        // unknown issue without If-Match: 404, not 428
        Tracker.put("TRK-99", null, Tracker.updateBody("x", "LOW", null)).then().statusCode(404);

        // closed issue with a stale version: 412 (reload first), not 409
        Tracker.advanceTo(key, "CLOSED");
        Tracker.put(key, "\"0\"", Tracker.updateBody("x", "LOW", "alice")).then().statusCode(412);
        // and with the right version: 409
        Tracker.put(key, Tracker.etag(key), Tracker.updateBody("x", "LOW", "alice")).then().statusCode(409);
    }

    @Test
    void putValidatesItsBody() {
        String etag = Tracker.etag(key);
        for (String body : new String[] { "{\"priority\":\"LOW\"}", "{\"title\":\"x\"}",
                "{\"title\":\"x\",\"priority\":\"LOW\",\"assignee\":\"\"}",
                "{\"title\":\"x\",\"priority\":\"LOW\",\"status\":\"CLOSED\"}" }) {
            Tracker.put(key, etag, body).then().statusCode(400);
        }
        Tracker.get(key).then().body("title", equalTo("Original"), "version", equalTo(0));
    }
}
