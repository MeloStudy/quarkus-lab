package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Task 5: tests that need no Keycloak token. {@code @TestSecurity} + {@code @OidcSecurity} install an identity and
 * claims without verifying anything: they prove the authorization rules, not the authentication.
 */
@QuarkusTest
class TestSecurityTest {

    @Test
    // TODO task 5: run this test as the user "rita" with the role "reader" (no token)
    void readerIsAllowedToRead() {
        given().get("/api/notes").then().statusCode(200);
    }

    @Test
    // TODO task 5: same user and role as above
    void readerIsRefusedWriting() {
        given().get("/api/notes").then().statusCode(200);
        given().contentType("application/json").body("{\"title\":\"x\"}").post("/api/notes").then().statusCode(403);
    }

    @Test
    // TODO task 5: same user and role, plus the claims email = "rita@mail.example"
    // and exp = 4102444800 (a number: ClaimType.LONG), both without a token
    void claimsCanBeSuppliedWithoutAToken() {
        given().get("/api/me").then().statusCode(200)
                .body("name", equalTo("rita"))
                .body("email", equalTo("rita@mail.example"));
    }

    @Test
    void withoutTheAnnotationsThereIsNoIdentity() {
        given().get("/api/me").then().statusCode(401);
    }
}
