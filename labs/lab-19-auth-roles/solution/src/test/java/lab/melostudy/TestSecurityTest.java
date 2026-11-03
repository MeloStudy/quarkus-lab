package lab.melostudy;

import static io.restassured.RestAssured.given;
import static lab.melostudy.Users.as;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

/**
 * Task 5: tests that need an identity but are not about logging in. {@code @TestSecurity} installs the identity
 * directly: no header, no password, no realm. It is the quick way to test authorization; it can NOT test
 * authentication (the tests of {@link AuthenticationTest} use real headers for that).
 */
@QuarkusTest
class TestSecurityTest {

    @Inject
    ReportStore store;

    @BeforeEach
    void emptyStore() {
        store.clear();
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void theIdentityComesFromTheAnnotationWithoutAnyHeader() {
        given().get("/api/me").then().statusCode(200).body("name", equalTo("bob"), "roles", contains("user"));
    }

    @Test
    @TestSecurity(user = "boss", roles = "admin")
    void theUserDoesNotEvenHaveToExistInTheRealm() {
        // "boss" is not in application.properties, and it still passes the admin check
        given().get("/admin/stats").then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void aUserIdentityStillMeetsTheAdminDoor() {
        given().get("/admin/stats").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void ownershipWorksWithAnInjectedIdentityToo() {
        store.add("carol", "carol's", "c");

        given().contentType("application/json").body("{\"title\":\"bob's\",\"body\":\"b\"}")
                .post("/api/reports").then().statusCode(201).body("owner", equalTo("bob"));

        given().get("/api/reports").then().body("title", contains("bob's"));
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void aRealHeaderIsStillProcessedAndWinsOverTheAnnotation() {
        // the annotation says bob, the header says alice: the request is authenticated as alice
        as("alice").get("/api/me").then().statusCode(200).body("name", equalTo("alice"));
        // and a wrong password is rejected even though the annotation would have let the request in
        given().auth().preemptive().basic("alice", "wrong").get("/api/me").then().statusCode(401);
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void withAuthorizationOffEveryAnnotationIsIgnored() {
        // handy to test business logic in isolation, dangerous as a habit: nothing here proves the doors are closed
        given().get("/api/reports").then().statusCode(200).body("$", empty());
        given().get("/admin/stats").then().statusCode(200);
    }
}
