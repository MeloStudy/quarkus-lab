package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * Task 5: a SECOND test class with the same profile class. Both classes share one strict application: the profile
 * is identified by its class, not by the configuration it happens to contain.
 */
@QuarkusTest
@TestProfile(StrictProfile.class)
class StrictModeHistoryTest {

    @BeforeEach
    @AfterEach
    void cleanHistory() {
        QuarkusTransaction.requiringNew().run(() -> Conversion.deleteAll());
    }

    @Test
    void aRefusedPairLeavesNoHistory() {
        given().queryParam("amount", "100").queryParam("from", "USD").queryParam("to", "PEN").get("/convert").then().statusCode(422);

        given().get("/history").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void anAcceptedPairIsRecorded() {
        given().queryParam("amount", "100").queryParam("from", "USD").queryParam("to", "GBP").get("/convert").then().statusCode(200);

        given().get("/history").then().statusCode(200).body("$", hasSize(1));
    }
}
