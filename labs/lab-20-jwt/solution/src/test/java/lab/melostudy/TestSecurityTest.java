package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.ClaimType;
import io.quarkus.test.security.jwt.JwtSecurity;

/**
 * Task 4: tests that skip token creation. {@code @TestSecurity} + {@code @JwtSecurity} inject an identity
 * and claims without signing or verifying anything, so keep some tests with real tokens (the other classes).
 */
@QuarkusTest
class TestSecurityTest {

    @Test
    @TestSecurity(user = "alice@lab.example", roles = "reader")
    void readerIsAllowedToRead() {
        given().get("/api/notes").then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "alice@lab.example", roles = "reader")
    void readerIsRefusedWriting() {
        given().get("/api/notes").then().statusCode(200);
        given().contentType("application/json").body("{\"title\":\"x\"}").post("/api/notes").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "alice@lab.example", roles = "reader")
    @JwtSecurity(claims = {
            @Claim(key = "email", value = "alice@mail.example"),
            @Claim(key = "exp", value = "4102444800", type = ClaimType.LONG) })
    void claimsCanBeSuppliedWithoutAToken() {
        given().get("/api/me").then().statusCode(200)
                .body("name", equalTo("alice@lab.example"))
                .body("email", equalTo("alice@mail.example"));
    }

    @Test
    void withoutTheAnnotationsThereIsNoIdentity() {
        given().get("/api/me").then().statusCode(401);
    }
}
