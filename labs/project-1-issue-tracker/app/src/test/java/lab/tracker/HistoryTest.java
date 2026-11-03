package lab.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** The audit trail: one line per real change, written with the change, and never for a change that did not happen. */
@QuarkusTest
class HistoryTest {

    @Inject
    TestData data;

    String key;

    @BeforeEach
    void anIssue() {
        data.reset();
        Tracker.project("TRK").then().statusCode(201);
        key = Tracker.issue("TRK", "Audited");
    }

    private static io.restassured.response.ValidatableResponse history(String key) {
        return given().get("/api/issues/{key}/history", key).then().statusCode(200);
    }

    @Test
    void creationIsTheFirstEvent() {
        history(key).body("kind", contains("CREATED"), "detail[0]", equalTo("Created as BUG with priority MEDIUM"));
    }

    @Test
    void everyKindOfChangeIsRecordedInOrderWithBeforeAndAfter() {
        Tracker.put(key, Tracker.etag(key), Tracker.updateBody("Audited", "CRITICAL", "alice")).then().statusCode(200);
        Tracker.move(key, "IN_PROGRESS").then().statusCode(200);
        Tracker.put(key, Tracker.etag(key), Tracker.updateBody("Audited", "CRITICAL", "bob")).then().statusCode(200);

        history(key).body(
                "kind", contains("CREATED", "ASSIGNEE_CHANGED", "PRIORITY_CHANGED", "STATUS_CHANGED", "ASSIGNEE_CHANGED"),
                "detail[1]", equalTo("unassigned -> alice"),
                "detail[2]", equalTo("MEDIUM -> CRITICAL"),
                "detail[3]", equalTo("OPEN -> IN_PROGRESS"),
                "detail[4]", equalTo("alice -> bob"));
    }

    @Test
    void eventsAreInChronologicalOrder() {
        Tracker.advanceTo(key, "CLOSED");

        List<String> times = history(key).extract().jsonPath().getList("at");
        List<Instant> parsed = times.stream().map(Instant::parse).toList();
        assertEquals(parsed.stream().sorted().toList(), parsed);
        assertEquals(5, parsed.size()); // created + assigned + three moves
    }

    @Test
    void editingOnlyTheTitleLeavesNoEvent() {
        Tracker.put(key, Tracker.etag(key), Tracker.updateBody("A better title", "MEDIUM", null)).then().statusCode(200);

        history(key).body("kind", contains("CREATED"));
    }

    @Test
    void refusedChangesLeaveNoTrace() {
        String stale = Tracker.etag(key);
        Tracker.move(key, "IN_PROGRESS").then().statusCode(409);                              // no assignee
        Tracker.transition(key, stale, "RESOLVED").then().statusCode(409);                     // invalid transition
        Tracker.put(key, "\"99\"", Tracker.updateBody("x", "HIGH", "alice")).then().statusCode(412); // stale
        Tracker.put(key, null, Tracker.updateBody("x", "HIGH", "alice")).then().statusCode(428);      // no precondition

        history(key).body("kind", contains("CREATED"));
        Tracker.get(key).then().body("version", equalTo(0), "priority", equalTo("MEDIUM"));
    }

    @Test
    void anUnknownIssueHasNoHistory() {
        given().get("/api/issues/TRK-99/history").then().statusCode(404);
    }
}
