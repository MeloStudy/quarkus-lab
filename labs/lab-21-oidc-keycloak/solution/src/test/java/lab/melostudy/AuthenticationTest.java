package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 1: the service trusts the tokens that Keycloak (the realm of Dev Services) signs. */
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
    void tokenFromKeycloakIsAccepted() {
        Api.as("rita").get("/api/me").then().statusCode(200)
                .body("name", equalTo("rita"))
                .body("email", equalTo("rita@lab.example"));
    }

    @Test
    void tokenIssuerIsTheRealmOfTheLab() {
        Api.as("rita").get("/api/me").then().statusCode(200)
                .body("issuer", endsWith("/realms/lab"));
    }

    @Test
    void garbageTokenIsUnauthorized() {
        Api.as("rita").get("/api/me").then().statusCode(200);
        Api.bearer("not-a-jwt").get("/api/me").then().statusCode(401);
    }

    @Test
    void tokenWithAlteredSignatureIsUnauthorized() {
        String token = Tokens.of("rita");
        Api.bearer(token).get("/api/me").then().statusCode(200);
        String altered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");
        Api.bearer(altered).get("/api/me").then().statusCode(401);
    }
}
