package lab.melostudy;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 3: a token of the same realm, correctly signed, but issued for another client. */
@QuarkusTest
class AudienceTest {

    @Test
    void tokenForThisServiceIsAccepted() {
        Api.as("rita").get("/api/me").then().statusCode(200);
    }

    @Test
    void tokenForAnotherClientIsUnauthorized() {
        Api.as("rita").get("/api/me").then().statusCode(200);
        Api.bearer(Tokens.forBilling("rita")).get("/api/me").then().statusCode(401);
    }

    @Test
    void tokenForAnotherClientCannotReadNotesEither() {
        Api.as("rita").get("/api/notes").then().statusCode(200);
        Api.bearer(Tokens.forBilling("rita")).get("/api/notes").then().statusCode(401);
    }
}
