package lab.melostudy;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Task 2, the third change: it only exists in the packaged application (dev and production modes), so this test runs against
 * the jar (it is an integration test: it runs in {@code verify}, not in {@code test}). The service listens on {@code localhost}
 * and a reverse proxy forwards the requests with their original {@code Host} header.
 */
@QuarkusIntegrationTest
class ReverseProxyIT {

    @Test
    void aRequestThatArrivesThroughAReverseProxyIsAccepted() {
        given().header("Host", "tickets.example.com").get("/tickets/t1").then().statusCode(200);
    }

    @Test
    void aRequestToLocalhostIsAccepted() {
        given().get("/tickets/t1").then().statusCode(200);
    }
}
