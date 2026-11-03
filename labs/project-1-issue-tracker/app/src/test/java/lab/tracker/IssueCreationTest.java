package lab.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
class IssueCreationTest {

    @Inject
    TestData data;

    @BeforeEach
    void projectsExist() {
        data.reset();
        Tracker.project("TRK").then().statusCode(201);
        Tracker.project("ABC").then().statusCode(201);
    }

    @Test
    void anIssueStartsOpenWithADefaultPriorityAndItsFirstVersion() {
        Tracker.open("TRK", "{\"title\":\"Login fails\",\"type\":\"BUG\"}").then().statusCode(201)
                .header("Location", endsWith("/api/issues/TRK-1"))
                .header("ETag", equalTo("\"0\""))
                .body("key", equalTo("TRK-1"), "project", equalTo("TRK"), "title", equalTo("Login fails"),
                        "type", equalTo("BUG"), "status", equalTo("OPEN"), "priority", equalTo("MEDIUM"),
                        "assignee", nullValue(), "labels", empty(), "version", equalTo(0),
                        "createdAt", notNullValue(), "updatedAt", notNullValue());
    }

    @Test
    void numbersCountPerProject() {
        assertEquals("TRK-1", Tracker.issue("TRK", "one"));
        assertEquals("TRK-2", Tracker.issue("TRK", "two"));
        assertEquals("ABC-1", Tracker.issue("ABC", "other project"));
        assertEquals("TRK-3", Tracker.issue("TRK", "three"));
    }

    @Test
    void labelsAndPriorityAreAccepted() {
        Tracker.open("TRK", "{\"title\":\"Slow\",\"type\":\"TASK\",\"priority\":\"HIGH\",\"labels\":[\"perf\",\"db\"]}")
                .then().statusCode(201).body("priority", equalTo("HIGH"), "labels", contains("db", "perf"));
    }

    @Test
    void theTitleIsTrimmed() {
        Tracker.open("TRK", "{\"title\":\"  Padded  \",\"type\":\"BUG\"}").then().statusCode(201)
                .body("title", equalTo("Padded"));
    }

    @Test
    void invalidInputIsReportedFieldByField() {
        Tracker.issue("TRK", "baseline"); // a valid request works, so the 400s are about the fields

        String[][] cases = {
                { "{\"title\":\" \",\"type\":\"BUG\"}", "title" },
                { "{\"title\":\"" + "x".repeat(121) + "\",\"type\":\"BUG\"}", "title" },
                { "{\"title\":\"ok\"}", "type" },
                { "{\"title\":\"ok\",\"type\":\"BUG\",\"description\":\"" + "x".repeat(2001) + "\"}", "description" },
                { "{\"title\":\"ok\",\"type\":\"BUG\",\"labels\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"]}", "labels" },
                { "{\"title\":\"ok\",\"type\":\"BUG\",\"labels\":[\"Not Valid\"]}", "labels" },
        };
        for (String[] c : cases) {
            Tracker.open("TRK", c[0]).then().statusCode(400)
                    .body("code", equalTo("VALIDATION_ERROR"), "errors.field", hasItem(c[1]));
        }
        assertEquals(1, data.count("issue"), "only the baseline was created");
    }

    @Test
    void theClientCannotChooseTheStatusOrAnythingElseItDoesNotOwn() {
        for (String field : new String[] { "\"status\":\"CLOSED\"", "\"version\":9", "\"key\":\"TRK-99\"" }) {
            Tracker.open("TRK", "{\"title\":\"ok\",\"type\":\"BUG\"," + field + "}").then().statusCode(400)
                    .body("code", equalTo("MALFORMED_REQUEST"));
        }
        assertEquals(0, data.count("issue"));
    }

    @Test
    void anUnknownEnumValueAndBrokenJsonAreMalformedRequests() {
        Tracker.open("TRK", "{\"title\":\"ok\",\"type\":\"EPIC\"}").then().statusCode(400)
                .body("code", equalTo("MALFORMED_REQUEST"));
        Tracker.open("TRK", "{\"title\":").then().statusCode(400).body("code", equalTo("MALFORMED_REQUEST"));
        given().contentType(ContentType.JSON).post("/api/projects/TRK/issues").then().statusCode(400);
    }

    @Test
    void anUnknownProjectIs404() {
        Tracker.open("NOPE", "{\"title\":\"ok\",\"type\":\"BUG\"}").then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    void aKeyThatCannotExistIs404NotAnError() {
        Tracker.issue("TRK", "exists");

        for (String key : new String[] { "TRK-2", "TRK-0", "trk-1", "abc", "TRK-99999999999", "T-1" }) {
            Tracker.get(key).then().statusCode(404).body("code", equalTo("NOT_FOUND"));
        }
        Tracker.get("TRK-1").then().statusCode(200);
    }
}
