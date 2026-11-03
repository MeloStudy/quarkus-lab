package lab.melostudy;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 3: the secret is used by the server and never shown by it. */
@QuarkusTest
class SecretsTest {

    @Test
    void partnerWithTheRightKeyGetsTheData() {
        Https.request().header("X-Api-Key", "test-only-key").get("/api/partner/data").then()
                .statusCode(200)
                .body("data", equalTo("partner data"));
    }

    @Test
    void partnerWithoutKeyIsUnauthorized() {
        Https.request().header("X-Api-Key", "test-only-key").get("/api/partner/data").then().statusCode(200);
        Https.request().get("/api/partner/data").then().statusCode(401);
    }

    @Test
    void partnerWithAWrongOrPartialKeyIsUnauthorized() {
        Https.request().header("X-Api-Key", "test-only-key").get("/api/partner/data").then().statusCode(200);
        Https.request().header("X-Api-Key", "wrong").get("/api/partner/data").then().statusCode(401);
        Https.request().header("X-Api-Key", "test-only-ke").get("/api/partner/data").then().statusCode(401);
    }

    @Test
    void infoShowsAFingerprintNotTheSecret() {
        Https.request().get("/api/info").then()
                .statusCode(200)
                .body("name", equalTo("Lab 22"))
                .body("apiKeyFingerprint", matchesPattern("[0-9a-f]{8}"))
                .body(not(containsString("test-only-key")));
    }
}
