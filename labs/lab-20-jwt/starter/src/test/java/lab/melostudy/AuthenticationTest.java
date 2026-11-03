package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 1: the service accepts only tokens signed by the trusted key and issued by the trusted issuer. */
@QuarkusTest
class AuthenticationTest {

    @Test
    void publicEndpointNeedsNoToken() {
        given().get("/api/public/status").then().statusCode(200).body(equalTo("up"));
    }

    @Test
    void protectedEndpointWithoutTokenIsUnauthorized() {
        given().get("/api/me").then().statusCode(401);
    }

    @Test
    void validTokenIsAccepted() {
        Api.as("alice@lab.example", "reader").get("/api/me").then().statusCode(200);
    }

    @Test
    void garbageTokenIsUnauthorized() {
        Api.as("alice@lab.example", "reader").get("/api/me").then().statusCode(200);
        Api.bearer("not-a-jwt").get("/api/me").then().statusCode(401);
    }

    @Test
    void tokenSignedWithAnotherKeyIsUnauthorized() {
        Api.as("alice@lab.example", "reader").get("/api/me").then().statusCode(200);
        Api.bearer(Tokens.forged("alice@lab.example", "admin")).get("/api/me").then().statusCode(401);
    }

    @Test
    void tokenFromAnotherIssuerIsUnauthorized() {
        Api.as("alice@lab.example", "reader").get("/api/me").then().statusCode(200);
        String token = Tokens.valid("alice@lab.example", "reader").issuer("https://evil.example/issuer").sign();
        Api.bearer(token).get("/api/me").then().statusCode(401);
    }

    @Test
    void expiredTokenIsUnauthorized() {
        Api.as("alice@lab.example", "reader").get("/api/me").then().statusCode(200);
        String token = Tokens.valid("alice@lab.example", "reader")
                .issuedAt(Instant.now().minusSeconds(600))
                .expiresAt(Instant.now().minusSeconds(300))
                .sign();
        Api.bearer(token).get("/api/me").then().statusCode(401);
    }

    @Test
    void tokenInAnotherSchemeIsUnauthorized() {
        Api.as("alice@lab.example", "reader").get("/api/me").then().statusCode(200);
        given().header("Authorization", "Basic " + Tokens.signed("alice@lab.example", "reader"))
                .get("/api/me").then().statusCode(401);
    }
}
