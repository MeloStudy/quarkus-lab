package lab.melostudy;

import static lab.melostudy.Users.anonymous;
import static lab.melostudy.Users.as;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Task 2: what may you do? Roles on the methods, and a closed door for whatever nobody annotated. */
@QuarkusTest
class RolesTest {

    @Inject
    ReportStore store;

    @BeforeEach
    void emptyStore() {
        store.clear();
    }

    @Test
    void anonymousIs401ButAWrongRoleIs403() {
        as("bob").get("/api/reports").then().statusCode(200); // the endpoint works for the right role

        anonymous().get("/api/reports").then().statusCode(401);   // "who are you?"
        as("dave").get("/api/reports").then().statusCode(403);    // "I know you, and the answer is no"
    }

    @Test
    void deletingIsForAdminsOnly() {
        Report report = store.add("bob", "to delete", "x");

        anonymous().delete("/api/reports/" + report.id()).then().statusCode(401);
        as("bob").delete("/api/reports/" + report.id()).then().statusCode(403);
        as("alice").delete("/api/reports/" + report.id()).then().statusCode(204);
    }

    @Test
    void aClassLevelRoleProtectsEveryMethodOfTheResource() {
        as("alice").get("/admin/stats").then().statusCode(200).body("reports", equalTo(0));

        as("bob").get("/admin/stats").then().statusCode(403);
        anonymous().get("/admin/stats").then().statusCode(401);
    }

    @Test
    void aMethodLevelAnnotationReplacesTheClassLevelOne() {
        // AdminResource says "admin" for the class and "admin or user" for this one method
        as("bob").get("/admin/health-note").then().statusCode(200).body("note", equalTo("visible to users too"));
        as("dave").get("/admin/health-note").then().statusCode(403);
    }

    @Test
    void anEndpointNobodyAnnotatedIsClosedNotOpen() {
        // DraftResource has no security annotation at all: the default must be "no", for everybody
        anonymous().get("/api/drafts").then().statusCode(401);
        as("bob").get("/api/drafts").then().statusCode(403);
        as("alice").get("/api/drafts").then().statusCode(403);
    }

    @Test
    void openIsADecisionWrittenDownWithPermitAll() {
        anonymous().get("/public/status").then().statusCode(200);
    }
}
