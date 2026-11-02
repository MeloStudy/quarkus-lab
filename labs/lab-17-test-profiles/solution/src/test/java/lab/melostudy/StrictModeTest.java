package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.ValidatableResponse;

/** Task 3: the same application with different configuration, in a test profile of its own. */
@QuarkusTest
@TestProfile(StrictProfile.class)
class StrictModeTest {

    @ConfigProperty(name = "app.strict")
    boolean strict;

    private static ValidatableResponse convert(String from, String to) {
        return given().queryParam("amount", "100").queryParam("from", from).queryParam("to", to)
                .when().get("/convert").then();
    }

    @Test
    void thisApplicationRunsInStrictMode() {
        assertTrue(strict);
    }

    @Test
    void aPairWithoutAFeeIsRefusedBeforeAskingTheRemoteService() {
        convert("USD", "PEN").statusCode(422);
    }

    @Test
    void aPairWithAFeeStillConverts() {
        // needs the fake rates server: the profile must bring it, because the test class does not declare it
        convert("USD", "EUR").statusCode(200).body("result", equalTo(91.08f));
    }
}
