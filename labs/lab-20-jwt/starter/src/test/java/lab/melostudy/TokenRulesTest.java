package lab.melostudy;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 5: audience and maximum lifetime. A token can be correctly signed and still not be for this API. */
@QuarkusTest
class TokenRulesTest {

    @BeforeEach
    void baselineWorks() {
        Api.as("alice@lab.example", "reader").get("/api/me").then().statusCode(200);
    }

    @Test
    void tokenForAnotherAudienceIsUnauthorized() {
        String token = Tokens.valid("alice@lab.example", "reader").audience("billing-api").sign();
        Api.bearer(token).get("/api/me").then().statusCode(401);
    }

    @Test
    void tokenWithoutAudienceIsUnauthorized() {
        String token = Tokens.valid("alice@lab.example", "reader").remove("aud").sign();
        Api.bearer(token).get("/api/me").then().statusCode(401);
    }

    @Test
    void tokenWithSeveralAudiencesIsAcceptedIfOneMatches() {
        String token = Tokens.valid("alice@lab.example", "reader")
                .audience(Set.of("billing-api", Tokens.AUDIENCE)).sign();
        Api.bearer(token).get("/api/me").then().statusCode(200);
    }

    @Test
    void tokenThatLivesTooLongIsUnauthorized() {
        String token = Tokens.valid("alice@lab.example", "reader").expiresIn(Duration.ofHours(24)).sign();
        Api.bearer(token).get("/api/me").then().statusCode(401);
    }

    @Test
    void tokenThatLivesLessThanTheLimitIsAccepted() {
        String token = Tokens.valid("alice@lab.example", "reader").expiresIn(Duration.ofMinutes(30)).sign();
        Api.bearer(token).get("/api/me").then().statusCode(200);
    }
}
