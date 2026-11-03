package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;

/** Task 4: a carrier that keeps failing is left alone for a while, and asked again later. */
@QuarkusTest
class CircuitBreakerTest extends FaultToleranceTestBase {

    @Test
    void breakerOpensAndStopsCallingTheCarrier() {
        warmCache("Lima");
        Carrier.alwaysFail();

        given().get("/api/shipping/Lima").then().statusCode(200).body("source", equalTo("cache"));
        given().get("/api/shipping/Lima").then().statusCode(200).body("source", equalTo("cache"));
        int callsThatOpenedIt = Carrier.calls();
        assertThat(CircuitBreakerMaintenance.get().currentState(ShippingService.BREAKER), equalTo(CircuitBreakerState.OPEN));

        for (int i = 0; i < 5; i++) {
            given().get("/api/shipping/Lima").then().statusCode(200).body("source", equalTo("cache"));
        }
        assertThat("while open, the carrier is not called at all", Carrier.calls(), equalTo(callsThatOpenedIt));
    }

    @Test
    void everyRetryCountsAsAFailureOfTheBreaker() {
        warmCache("Lima");
        Carrier.alwaysFail();
        given().get("/api/shipping/Lima").then().statusCode(200);
        given().get("/api/shipping/Lima").then().statusCode(200);
        // volume threshold 4: the first request made 3 attempts, the second made 1 more and the circuit opened
        assertThat(Carrier.calls(), equalTo(4));
    }

    @Test
    void breakerClosesAgainWhenTheCarrierRecovers() throws InterruptedException {
        warmCache("Lima");
        Carrier.alwaysFail();
        given().get("/api/shipping/Lima").then().statusCode(200);
        given().get("/api/shipping/Lima").then().statusCode(200);
        assertThat(CircuitBreakerMaintenance.get().currentState(ShippingService.BREAKER), equalTo(CircuitBreakerState.OPEN));

        Carrier.heal();
        Thread.sleep(1300); // the test profile keeps the breaker open for 1 second
        given().get("/api/shipping/Lima").then().statusCode(200).body("source", equalTo("carrier"));
        assertThat(CircuitBreakerMaintenance.get().currentState(ShippingService.BREAKER), equalTo(CircuitBreakerState.CLOSED));
    }

    @Test
    void mistakesOfTheCallerDoNotOpenTheBreaker() {
        given().get("/api/shipping/Lima").then().statusCode(200);
        for (int i = 0; i < 10; i++) {
            given().get("/api/shipping/Atlantis").then().statusCode(404);
        }
        assertThat(CircuitBreakerMaintenance.get().currentState(ShippingService.BREAKER), equalTo(CircuitBreakerState.CLOSED));
        given().get("/api/shipping/Quito").then().statusCode(200).body("source", equalTo("carrier"));
    }
}
