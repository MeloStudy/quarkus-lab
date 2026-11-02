package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;

/** Task 2: the whole path with a fake remote service that a test resource starts before the application. */
@QuarkusTest
@WithTestResource(FakeRatesResource.class)
class ConversionTest {

    @BeforeEach
    @AfterEach
    void cleanHistory() {
        QuarkusTransaction.requiringNew().run(() -> Conversion.deleteAll());
    }

    private static ValidatableResponse convert(String amount, String from, String to) {
        return given().queryParam("amount", amount).queryParam("from", from).queryParam("to", to)
                .when().get("/convert").then();
    }

    @Test
    void aConversionUsesTheRemoteRateAndTheStoredFee() {
        // 100 x 0.92 = 92.00, minus the 1.00 % fee of USD -> EUR = 91.08
        convert("100", "USD", "EUR").statusCode(200)
                .body("rate", equalTo(0.92f))
                .body("feePercent", equalTo(1.00f))
                .body("result", equalTo(91.08f));
    }

    @Test
    void aPairWithoutAFeeIsConvertedWithNoFeeWhenNotStrict() {
        convert("100", "USD", "PEN").statusCode(200).body("feePercent", equalTo(0)).body("result", equalTo(370.00f));
    }

    @Test
    void everyConversionIsRecorded() {
        convert("100", "USD", "EUR").statusCode(200);
        convert("10", "EUR", "USD").statusCode(200);

        given().get("/history").then().statusCode(200).body("$", hasSize(2)).body("[0].result", equalTo(91.08f));
    }

    @Test
    void aPairTheRemoteServiceDoesNotKnowIs404() {
        convert("100", "USD", "EUR").statusCode(200); // the fake server is reachable...
        convert("100", "EUR", "GBP").statusCode(404); // ...and does not know this pair
    }

    @Test
    void whenTheRemoteServiceFailsTheAnswerIs503AndNothingIsRecorded() {
        convert("100", "USD", "EUR").statusCode(200); // ...the happy path works, so the 503 below is about the failure
        given().get("/history").then().body("$", hasSize(1));

        convert("100", "XAU", "EUR").statusCode(503); // pairs starting with X make the fake server answer 500

        given().get("/history").then().body("$", hasSize(1));
    }

    @Test
    void invalidInputIs400() {
        convert("100", "USD", "EUR").statusCode(200);
        convert("0", "USD", "EUR").statusCode(400);
        convert("100", "usd", "EUR").statusCode(400);
    }
}
