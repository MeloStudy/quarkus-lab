package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Lab 07, tasks 1-2: the price endpoint consumes the remote service and answers with our contract. */
@QuarkusTest
@QuarkusTestResource(RatesStubResource.class)
class PriceConversionTest {

    @Inject
    ProductRepository repository;

    @BeforeEach
    void resetData() {
        repository.reset();
    }

    // ---- Task 1: the happy path

    @Test
    void convertsThePriceWithTheRemoteRate() {
        // product 1 costs 30.50 USD; 30.50 * 0.92 = 28.06
        given().queryParam("currency", "EUR")
                .when().get("/products/1/price")
                .then().statusCode(200)
                .body("productId", equalTo(1))
                .body("currency", equalTo("EUR"))
                .body("rate", equalTo(0.92f))
                .body("amount", equalTo(28.06f));
    }

    // ---- Task 2: remote failures become OUR contract

    @Test
    void unknownCurrencyIsAClientError() {
        given().queryParam("currency", "XXX")
                .when().get("/products/1/price")
                .then().statusCode(400)
                .body("code", equalTo("UNSUPPORTED_CURRENCY"));
    }

    @Test
    void remoteServerErrorBecomesBadGateway() {
        given().queryParam("currency", "ERR")
                .when().get("/products/1/price")
                .then().statusCode(502)
                .body("code", equalTo("UPSTREAM_ERROR"));
    }

    @Test
    void unusableAnswersBecomeBadGateway() {
        for (String currency : new String[] { "BAD", "ZER" }) {
            given().queryParam("currency", currency)
                    .when().get("/products/1/price")
                    .then().statusCode(502)
                    .body("code", equalTo("UPSTREAM_ERROR"));
        }
    }

    @Test
    void errorBodiesNeverRevealTheRemoteDetails() {
        String body = given().queryParam("currency", "ERR")
                .when().get("/products/1/price")
                .then().statusCode(502) // first prove the failure path ran, otherwise the checks below pass vacuously
                .extract().asString();
        assertThat(body, not(containsString("boom")));
        assertThat(body, not(containsString("localhost")));
    }

    // ---- Our own validation and existence checks still come first

    @Test
    void unknownProductIs404WithoutCallingTheRemote() {
        // "SLO" would take 3 s: a fast 404 proves the remote was never called
        long start = System.nanoTime();
        given().queryParam("currency", "SLO")
                .when().get("/products/999/price")
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
        assertThat((System.nanoTime() - start) / 1_000_000, lessThan(1500L));
    }

    @Test
    void currencyMustLookLikeACurrency() {
        given().queryParam("currency", "euro")
                .when().get("/products/1/price")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("errors", hasSize(1))
                .body("errors[0].field", equalTo("currency"));
        given().when().get("/products/1/price")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }
}
