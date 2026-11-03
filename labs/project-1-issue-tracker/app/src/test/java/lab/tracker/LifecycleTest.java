package lab.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** The state machine as a client sees it: what moves, what is refused, and that a refusal changes nothing. */
@QuarkusTest
class LifecycleTest {

    @Inject
    TestData data;

    String key;

    @BeforeEach
    void anOpenIssue() {
        data.reset();
        Tracker.project("TRK").then().statusCode(201);
        key = Tracker.issue("TRK", "Something broke");
    }

    @Test
    void anIssueTravelsFromOpenToClosed() {
        Tracker.assign(key, "alice");

        Tracker.move(key, "IN_PROGRESS").then().statusCode(200).body("status", equalTo("IN_PROGRESS"));
        Tracker.move(key, "RESOLVED").then().statusCode(200).body("status", equalTo("RESOLVED"));
        Tracker.move(key, "CLOSED").then().statusCode(200).body("status", equalTo("CLOSED"));
        Tracker.get(key).then().body("status", equalTo("CLOSED"), "assignee", equalTo("alice"));
    }

    @Test
    void aResolvedIssueCanBeReopened() {
        Tracker.advanceTo(key, "RESOLVED");

        Tracker.move(key, "OPEN").then().statusCode(200).body("status", equalTo("OPEN"));
    }

    @Test
    void workCannotStartWithoutSomeoneToDoIt() {
        Tracker.move(key, "IN_PROGRESS").then().statusCode(409).body("code", equalTo("ASSIGNEE_REQUIRED"));

        Tracker.get(key).then().body("status", equalTo("OPEN"));
    }

    @Test
    void movesThatSkipAStepAreRefused() {
        Tracker.assign(key, "alice");

        // from OPEN
        Tracker.move(key, "RESOLVED").then().statusCode(409).body("code", equalTo("INVALID_TRANSITION"));
        Tracker.move(key, "CLOSED").then().statusCode(409).body("code", equalTo("INVALID_TRANSITION"));
        Tracker.move(key, "OPEN").then().statusCode(409).body("code", equalTo("INVALID_TRANSITION"));
        // from IN_PROGRESS
        Tracker.move(key, "IN_PROGRESS").then().statusCode(200);
        Tracker.move(key, "CLOSED").then().statusCode(409).body("code", equalTo("INVALID_TRANSITION"));
        // from RESOLVED
        Tracker.move(key, "RESOLVED").then().statusCode(200);
        Tracker.move(key, "IN_PROGRESS").then().statusCode(409).body("code", equalTo("INVALID_TRANSITION"));

        Tracker.get(key).then().body("status", equalTo("RESOLVED"));
    }

    @Test
    void closedIsFinal() {
        Tracker.advanceTo(key, "CLOSED");

        for (String to : new String[] { "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED" }) {
            Tracker.move(key, to).then().statusCode(409).body("code", equalTo("INVALID_TRANSITION"));
        }
    }

    @Test
    void aClosedIssueIsReadOnly() {
        Tracker.advanceTo(key, "CLOSED");

        Tracker.put(key, Tracker.etag(key), Tracker.updateBody("Edited", "HIGH", "alice")).then().statusCode(409)
                .body("code", equalTo("ISSUE_CLOSED"));
        given().contentType(ContentType.JSON).body("{\"author\":\"bob\",\"body\":\"late\"}")
                .post("/api/issues/{key}/comments", key).then().statusCode(409).body("code", equalTo("ISSUE_CLOSED"));
        Tracker.get(key).then().body("title", equalTo("Something broke"));
    }

    @Test
    void whoIsWorkingOnAnIssueCannotBeRemovedWhileItIsInProgress() {
        Tracker.advanceTo(key, "IN_PROGRESS");

        Tracker.put(key, Tracker.etag(key), Tracker.updateBody("Something broke", "MEDIUM", null)).then()
                .statusCode(409).body("code", equalTo("ASSIGNEE_REQUIRED"));
        Tracker.put(key, Tracker.etag(key), Tracker.updateBody("Something broke", "MEDIUM", "bob")).then()
                .statusCode(200).body("assignee", equalTo("bob"));
    }

    @Test
    void anUnknownTargetStatusIsAMalformedRequestAndAMissingOneIsAValidationError() {
        Tracker.transition(key, Tracker.etag(key), "DONE").then().statusCode(400)
                .body("code", equalTo("MALFORMED_REQUEST"));
        given().contentType(ContentType.JSON).header("If-Match", Tracker.etag(key)).body("{}")
                .post("/api/issues/{key}/transitions", key).then().statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void theDatabaseDoesNotLetAStatusInProgressWithoutAssigneeExistEvenIfTheApiIsBypassed() {
        long before = data.count("issue");
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> data.execute("UPDATE issue SET status = 'IN_PROGRESS' WHERE assignee IS NULL"));
        assertEquals(before, data.count("issue"));
        Tracker.get(key).then().body("status", equalTo("OPEN"));
    }
}
