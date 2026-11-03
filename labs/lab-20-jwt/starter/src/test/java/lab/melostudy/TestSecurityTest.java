package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Task 4: tests that skip token creation. {@code @TestSecurity} + {@code @JwtSecurity} inject an identity
 * and claims without signing or verifying anything, so keep some tests with real tokens (the other classes).
 */
@QuarkusTest
class TestSecurityTest {

    @Test
    // TODO task 4: run this test as the user "alice@lab.example" with the role "reader" (no token)
    void readerIsAllowedToRead() {
        given().get("/api/notes").then().statusCode(200);
    }

    @Test
    // TODO task 4: same user and role as above
    void readerIsRefusedWriting() {
        given().get("/api/notes").then().statusCode(200);
        given().contentType("application/json").body("{\"title\":\"x\"}").post("/api/notes").then().statusCode(403);
    }

    @Test
    // TODO task 4: same user and role, plus the claims email = "alice@mail.example"
    // and exp = 4102444800 (a number: ClaimType.LONG), both without creating a token
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
