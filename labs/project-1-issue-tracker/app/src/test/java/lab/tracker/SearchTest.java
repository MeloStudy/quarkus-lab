package lab.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;

/**
 * The list endpoint. The data set (created in this order, so the numbers are known):
 * <pre>
 * TRK-1 "Login page returns 500"   BUG   CRITICAL  alice  [auth, security]  description mentions "password"
 * TRK-2 "Add dark mode"            STORY LOW       -      [ui]
 * TRK-3 "Upgrade the driver"       TASK  MEDIUM    bob    []                 -> RESOLVED
 * TRK-4 "Discount is 100% wrong"   BUG   HIGH      -      []
 * ABC-1 "Login analytics"          TASK  LOW       -      [auth]
 * </pre>
 */
@QuarkusTest
class SearchTest {

    @Inject
    TestData data;

    @Inject
    QueryCounter queries;

    @BeforeEach
    void dataSet() {
        data.reset();
        Tracker.project("TRK").then().statusCode(201);
        Tracker.project("ABC").then().statusCode(201);
        Tracker.open("TRK", "{\"title\":\"Login page returns 500\",\"type\":\"BUG\",\"priority\":\"CRITICAL\","
                + "\"description\":\"Fails when the Password has a trailing space\",\"labels\":[\"auth\",\"security\"]}")
                .then().statusCode(201);
        Tracker.open("TRK", "{\"title\":\"Add dark mode\",\"type\":\"STORY\",\"priority\":\"LOW\",\"labels\":[\"ui\"]}")
                .then().statusCode(201);
        Tracker.open("TRK", "{\"title\":\"Upgrade the driver\",\"type\":\"TASK\"}").then().statusCode(201);
        Tracker.open("TRK", "{\"title\":\"Discount is 100% wrong\",\"type\":\"BUG\",\"priority\":\"HIGH\"}")
                .then().statusCode(201);
        Tracker.open("ABC", "{\"title\":\"Login analytics\",\"type\":\"TASK\",\"priority\":\"LOW\",\"labels\":[\"auth\"]}")
                .then().statusCode(201);
        Tracker.assign("TRK-1", "alice");
        Tracker.assign("TRK-3", "bob");
        Tracker.move("TRK-3", "IN_PROGRESS").then().statusCode(200);
        Tracker.move("TRK-3", "RESOLVED").then().statusCode(200);
    }

    private static Response list(String query) {
        // the query is written already encoded ("%25" is a percent sign): do not let RestAssured encode it twice
        return given().urlEncodingEnabled(false).get("/api/issues" + query);
    }

    @Test
    void withoutFiltersEverythingComesNewestFirstWithItsTotal() {
        list("").then().statusCode(200).header("X-Total-Count", "5")
                .body("key", contains("ABC-1", "TRK-4", "TRK-3", "TRK-2", "TRK-1"));
    }

    @Test
    void eachFilterNarrowsTheList() {
        list("?project=ABC").then().body("key", contains("ABC-1"));
        list("?status=RESOLVED").then().body("key", contains("TRK-3"));
        list("?status=OPEN").then().body("key", containsInAnyOrder("TRK-1", "TRK-2", "TRK-4", "ABC-1"));
        list("?assignee=alice").then().body("key", contains("TRK-1"));
        list("?priority=LOW").then().body("key", containsInAnyOrder("TRK-2", "ABC-1"));
        list("?label=auth").then().body("key", containsInAnyOrder("TRK-1", "ABC-1"));
        list("?label=nothing").then().body("key", empty());
    }

    @Test
    void filtersCombine() {
        list("?project=TRK&label=auth").then().body("key", contains("TRK-1"));
        list("?project=TRK&status=OPEN&priority=LOW").then().body("key", contains("TRK-2"));
    }

    @Test
    void textSearchLooksAtTitleAndDescriptionIgnoringCase() {
        list("?text=LOGIN").then().body("key", containsInAnyOrder("TRK-1", "ABC-1"));
        list("?text=password").then().body("key", contains("TRK-1")); // only in the description, other case
    }

    @Test
    void aPercentSignInTheSearchMeansItself() {
        list("?text=%25").then().body("key", contains("TRK-4")); // "%" is a character, not a wildcard
        list("?text=_").then().body("key", empty());
    }

    @Test
    void priorityIsSortedByImportanceNotAlphabetically() {
        // alphabetical would be CRITICAL, HIGH, LOW, MEDIUM
        list("?project=TRK&sort=-priority").then().body("priority", contains("CRITICAL", "HIGH", "MEDIUM", "LOW"));
        list("?project=TRK&sort=priority").then().body("priority", contains("LOW", "MEDIUM", "HIGH", "CRITICAL"));
    }

    @Test
    void sortByCreationBothWays() {
        list("?project=TRK&sort=created").then().body("key", contains("TRK-1", "TRK-2", "TRK-3", "TRK-4"));
        list("?project=TRK&sort=-created").then().body("key", contains("TRK-4", "TRK-3", "TRK-2", "TRK-1"));
    }

    @Test
    void sortByLastUpdateShowsTheMostRecentlyTouchedFirst() {
        Tracker.assign("TRK-2", "carol"); // the oldest issue is now the freshest
        list("?project=TRK&sort=-updated").then().body("key[0]", equalTo("TRK-2"));
    }

    @Test
    void pagesAreStableAndTheTotalIsTheWholeSet() {
        Response first = list("?sort=created&size=2&page=0");
        Response second = list("?sort=created&size=2&page=1");
        Response third = list("?sort=created&size=2&page=2");
        Response beyond = list("?sort=created&size=2&page=3");

        first.then().body("key", contains("TRK-1", "TRK-2"));
        second.then().body("key", contains("TRK-3", "TRK-4"));
        third.then().body("key", contains("ABC-1"));
        beyond.then().statusCode(200).body("key", empty());
        for (Response r : new Response[] { first, second, third, beyond }) {
            assertEquals(5, Tracker.count(r), "the total does not depend on the page");
        }
    }

    @Test
    void theSortAndThePagingAreWhitelistedAndBounded() {
        list("?sort=title").then().statusCode(400).body("errors.field", hasItem("sort"));
        list("?sort=id%3Bdrop%20table%20issue").then().statusCode(400);
        list("?size=0").then().statusCode(400).body("errors.field", hasItem("size"));
        list("?size=101").then().statusCode(400).body("errors.field", hasItem("size"));
        list("?page=-1").then().statusCode(400).body("errors.field", hasItem("page"));
        list("?project=lowercase").then().statusCode(400).body("errors.field", hasItem("project"));
        assertEquals(5, data.count("issue"));
        list("?size=100").then().statusCode(200); // the limit itself is fine
    }

    @Test
    void aPageOfIssuesDoesNotCostOneQueryPerIssue() {
        for (int i = 0; i < 40; i++) {
            Tracker.open("ABC", "{\"title\":\"bulk " + i + "\",\"type\":\"TASK\",\"labels\":[\"l" + i + "\"]}")
                    .then().statusCode(201);
        }
        queries.reset();

        list("?project=ABC&size=40").then().statusCode(200);

        // count + page (project joined in) + the labels of all 40 issues in ONE batch (@BatchSize(50) on Issue.labels)
        long statements = queries.statements();
        assertTrue(statements <= 3, "expected at most 3 statements for 40 issues but there were " + statements);
    }

    @Test
    void aFilterValueThatMakesNoSenseIs400NamingTheParameterNot404() {
        // JAX-RS would answer 404 for a query parameter it cannot convert; the list route does exist
        list("?status=DONE").then().statusCode(400).body("errors.field", hasItem("status"));
        list("?priority=URGENT").then().statusCode(400).body("errors.field", hasItem("priority"));
        list("?page=abc").then().statusCode(400).body("errors.field", hasItem("page"));
        list("?size=1.5").then().statusCode(400).body("errors.field", hasItem("size"));
    }

    @Test
    void everyStatusAndPriorityTheDomainHasIsAcceptedAsAFilter() {
        for (Status s : Status.values()) {
            list("?status=" + s).then().statusCode(200);
        }
        for (Priority p : Priority.values()) {
            list("?priority=" + p).then().statusCode(200);
        }
    }
}
