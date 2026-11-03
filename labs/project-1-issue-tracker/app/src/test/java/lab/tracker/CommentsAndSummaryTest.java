package lab.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

@QuarkusTest
class CommentsAndSummaryTest {

    @Inject
    TestData data;

    @BeforeEach
    void aProject() {
        data.reset();
        Tracker.project("TRK").then().statusCode(201);
    }

    private static Response comment(String key, String author, String body) {
        return given().contentType(ContentType.JSON)
                .body("{\"author\":\"" + author + "\",\"body\":\"" + body + "\"}")
                .post("/api/issues/{key}/comments", key);
    }

    @Test
    void commentsAreAddedAndListedOldestFirst() {
        String key = Tracker.issue("TRK", "Discussed");

        comment(key, "alice", "first").then().statusCode(201)
                .body("id", notNullValue(), "author", equalTo("alice"), "createdAt", notNullValue());
        comment(key, "bob", "  second  ").then().statusCode(201);

        given().get("/api/issues/{key}/comments", key).then().statusCode(200)
                .body("body", contains("first", "second"), "author", contains("alice", "bob"));
    }

    @Test
    void commentsAreValidated() {
        String key = Tracker.issue("TRK", "Discussed");
        comment(key, "alice", "baseline").then().statusCode(201);

        comment(key, " ", "text").then().statusCode(400).body("errors.field", hasItem("author"));
        comment(key, "alice", " ").then().statusCode(400).body("errors.field", hasItem("body"));
        comment(key, "alice", "x".repeat(1001)).then().statusCode(400).body("errors.field", hasItem("body"));
        given().get("/api/issues/{key}/comments", key).then().body("size()", equalTo(1));
    }

    @Test
    void anUnknownIssueCannotBeCommentedOrRead() {
        comment("TRK-9", "alice", "hello").then().statusCode(404);
        given().get("/api/issues/TRK-9/comments").then().statusCode(404);
    }

    @Test
    void theSummaryCountsEveryStatusAndTheOpenWorkByPriority() {
        Tracker.project("EMP").then().statusCode(201);
        String a = Tracker.issue("TRK", "a"); // MEDIUM, OPEN
        String b = Tracker.issue("TRK", "b"); // MEDIUM, IN_PROGRESS
        String c = Tracker.issue("TRK", "c"); // MEDIUM, CLOSED
        Tracker.advanceTo(b, "IN_PROGRESS");
        Tracker.advanceTo(c, "CLOSED");
        Tracker.put(a, Tracker.etag(a), Tracker.updateBody("a", "CRITICAL", null)).then().statusCode(200);

        given().get("/api/projects/TRK/summary").then().statusCode(200)
                .body("project", equalTo("TRK"), "total", equalTo(3),
                        "byStatus.OPEN", equalTo(1), "byStatus.IN_PROGRESS", equalTo(1),
                        "byStatus.RESOLVED", equalTo(0), "byStatus.CLOSED", equalTo(1),
                        "byPriority.CRITICAL", equalTo(1), "byPriority.MEDIUM", equalTo(1),
                        "byPriority.HIGH", equalTo(0), "byPriority.LOW", equalTo(0));
        // a project without issues reports zeros instead of missing keys
        given().get("/api/projects/EMP/summary").then().body("total", equalTo(0), "byStatus.OPEN", equalTo(0),
                "byPriority.CRITICAL", equalTo(0));
    }
}
