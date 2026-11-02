package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/** Lab 07, task 4: nobody is listening at the configured URL (the service is down). */
@QuarkusTest
@TestProfile(UnreachableRemoteTest.NothingListening.class)
class UnreachableRemoteTest {

    /** Points the client at a local port where nothing listens (port 1 is reserved and never open). */
    public static class NothingListening implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest-client.rates.url", "http://localhost:1");
        }
    }

    @Test
    void connectionRefusedBecomesBadGateway() {
        given().queryParam("currency", "EUR")
                .when().get("/products/1/price")
                .then().statusCode(502)
                .body("code", equalTo("UPSTREAM_ERROR"));
    }
}
