package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 3: when the carrier cannot help, answer with the last known price, but never invent one. */
@QuarkusTest
class FallbackTest extends FaultToleranceTestBase {

    @Test
    void lastKnownPriceIsServedWhileTheCarrierIsDown() {
        given().get("/api/shipping/Lima").then().statusCode(200).body("source", equalTo("carrier"));
        Carrier.alwaysFail();
        given().get("/api/shipping/Lima").then()
                .statusCode(200)
                .body("price", equalTo(12.5f))
                .body("source", equalTo("cache"));
    }

    @Test
    void withoutAPreviousPriceTheAnswerIsAnHonestError() {
        given().get("/api/shipping/Lima").then().statusCode(200);
        Carrier.alwaysFail();
        given().get("/api/shipping/Quito").then().statusCode(503).body("code", equalTo("SHIPPING_UNAVAILABLE"));
    }

    @Test
    void anUnknownCityIsNeverAnsweredFromTheCache() {
        given().get("/api/shipping/Lima").then().statusCode(200);
        given().get("/api/shipping/Atlantis").then().statusCode(404).body("code", equalTo("UNKNOWN_CITY"));
    }

    @Test
    void freshPricesComeBackWhenTheCarrierRecovers() {
        given().get("/api/shipping/Lima").then().statusCode(200);
        Carrier.alwaysFail();
        given().get("/api/shipping/Lima").then().statusCode(200).body("source", equalTo("cache"));
        Carrier.heal();
        shippingBreakerClosed();
        given().get("/api/shipping/Lima").then().statusCode(200).body("source", equalTo("carrier"));
    }

    private void shippingBreakerClosed() {
        io.smallrye.faulttolerance.api.CircuitBreakerMaintenance.get().resetAll();
    }
}
