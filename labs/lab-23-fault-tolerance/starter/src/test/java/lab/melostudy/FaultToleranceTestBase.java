package lab.melostudy;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.test.common.QuarkusTestResource;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;

/** Every test starts with a healthy carrier, an empty cache and a closed circuit breaker. */
@QuarkusTestResource(CarrierStub.class)
abstract class FaultToleranceTestBase {

    @Inject
    ShippingService shipping;

    @BeforeEach
    void startClean() {
        Carrier.reset();
        shipping.clearCache();
        CircuitBreakerMaintenance.get().resetAll();
    }

    /** Makes the service learn the price of a city, then forgets how that call went (the carrier and the breaker start clean). */
    protected void warmCache(String city) {
        io.restassured.RestAssured.given().get("/api/shipping/" + city).then().statusCode(200);
        Carrier.reset();
        CircuitBreakerMaintenance.get().resetAll();
    }
}
