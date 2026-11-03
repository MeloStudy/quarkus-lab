package lab.melostudy;

import static lab.melostudy.Users.as;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Task 3: whose data is it? A rule that only the code can enforce, using the identity of the caller. */
@QuarkusTest
class OwnershipTest {

    @Inject
    ReportStore store;

    Report bobs;
    Report carols;

    @BeforeEach
    void twoReports() {
        store.clear();
        bobs = store.add("bob", "bob's report", "b");
        carols = store.add("carol", "carol's report", "c");
    }

    @Test
    void aUserListsOnlyTheirOwnReports() {
        as("bob").get("/api/reports").then().statusCode(200).body("title", contains("bob's report"));
        as("carol").get("/api/reports").then().statusCode(200).body("title", contains("carol's report"));
    }

    @Test
    void anAdminSeesEverything() {
        as("alice").get("/api/reports").then().statusCode(200).body("owner", contains("bob", "carol"));
    }

    @Test
    void aUserReadsTheirOwnReportByIdButSomebodyElsesIsNotFound() {
        as("bob").get("/api/reports/" + bobs.id()).then().statusCode(200).body("owner", equalTo("bob"));

        // 404, not 403: a 403 would confirm that report N exists and belongs to somebody
        as("bob").get("/api/reports/" + carols.id()).then().statusCode(404);
        as("bob").get("/api/reports/9999").then().statusCode(404); // exactly the same answer as a report that is not there
    }

    @Test
    void anAdminReadsAnyReport() {
        as("alice").get("/api/reports/" + carols.id()).then().statusCode(200).body("owner", equalTo("carol"));
    }

    @Test
    void theOwnerIsTheAuthenticatedUserNotWhatTheBodySays() {
        as("bob").contentType(ContentType.JSON)
                .body("{\"title\":\"mine\",\"body\":\"x\",\"owner\":\"carol\"}")
                .post("/api/reports").then().statusCode(201)
                .header("Location", endsWith("/api/reports/" + (carols.id() + 1)))
                .body("owner", equalTo("bob"));

        as("carol").get("/api/reports").then().body("title", contains("carol's report")); // nothing slipped into carol's list
    }

    @Test
    void aReportNeedsATitle() {
        as("bob").contentType(ContentType.JSON).body("{\"title\":\"  \",\"body\":\"x\"}")
                .post("/api/reports").then().statusCode(400);
        as("bob").get("/api/reports").then().body("title", contains("bob's report")); // nothing was created
    }

    @Test
    void anAdminCanDeleteAndTheReportIsGone() {
        as("alice").delete("/api/reports/" + bobs.id()).then().statusCode(204);

        as("bob").get("/api/reports").then().body("$", empty());
        as("alice").delete("/api/reports/" + bobs.id()).then().statusCode(404);
    }
}
