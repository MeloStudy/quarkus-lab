package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;

/**
 * Task 5: tests that need no Keycloak token. {@code @TestSecurity} + {@code @OidcSecurity} install an identity and
 * claims without verifying anything: they prove the authorization rules, not the authentication.
 */
@QuarkusTest
class TestSecurityTest {

    @Test
    @TestSecurity(user = "rita", roles = "reader")
    void readerIsAllowedToRead() {
        given().get("/api/notes").then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "rita", roles = "reader")
    void readerIsRefusedWriting() {
        given().get("/api/notes").then().statusCode(200);
        given().contentType("application/json").body("{\"title\":\"x\"}").post("/api/notes").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "rita", roles = "reader")
    @OidcSecurity(claims = {
            @Claim(key = "email", value = "rita@mail.example"),
            @Claim(key = "exp", value = "4102444800", type = ClaimType.LONG) })
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
