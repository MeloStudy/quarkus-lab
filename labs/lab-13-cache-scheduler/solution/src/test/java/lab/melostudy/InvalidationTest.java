package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Task 2: the cache must not outlive the truth. Invalidation on writes, and a TTL for the writes nobody tells us about. */
@QuarkusTest
class InvalidationTest {

    @Inject
    RateTestSupport support;

    @Inject
    RateProvider provider;

    @BeforeEach
    void reset() {
        support.reset();
    }

    private static void put(String currency, String rate) {
        given().contentType(ContentType.JSON).body("{\"rate\":" + rate + "}")
                .when().put("/rates/" + currency).then().statusCode(200);
    }

    @Test
    void aChangedRateIsVisibleImmediately() {
        given().when().get("/rates/EUR").then().body("rate", equalTo(1.0f)); // now cached
        put("EUR", "2.00");
        given().when().get("/rates/EUR").then().body("rate", equalTo(2.0f));
    }

    @Test
    void changingOneRateKeepsTheOthersCached() {
        given().when().get("/rates/EUR").then().statusCode(200);
        given().when().get("/rates/USD").then().statusCode(200);
        assertThat(provider.calls(), equalTo(2));

        put("EUR", "2.00");

        given().when().get("/rates/USD").then().statusCode(200);
        assertThat(provider.calls(), equalTo(2)); // USD was not thrown away
        given().when().get("/rates/EUR").then().body("rate", equalTo(2.0f));
        assertThat(provider.calls(), equalTo(3)); // EUR was
    }

    @Test
    void aWriteBehindTheCacheBecomesVisibleOnlyAfterTheTtl() {
        given().when().get("/rates/EUR").then().body("rate", equalTo(1.0f));
        provider.update("EUR", new BigDecimal("3.00")); // someone else changed it upstream; nobody invalidated

        given().when().get("/rates/EUR").then().body("rate", equalTo(1.0f)); // stale, by design

        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> given().when().get("/rates/EUR").then().body("rate", equalTo(3.0f)));
    }
}
