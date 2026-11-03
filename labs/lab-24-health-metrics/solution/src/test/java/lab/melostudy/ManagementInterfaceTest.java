package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 1: operations endpoints are not part of the public API. They live on the management interface. */
@QuarkusTest
class ManagementInterfaceTest extends OpsTestBase {

    @Test
    void businessApiStaysOnThePublicPort() {
        given().contentType("application/json").body("{\"sku\":\"A-100\",\"quantity\":1,\"type\":\"standard\"}")
                .post("/api/orders").then().statusCode(201);
    }

    @Test
    void healthAndMetricsAnswerOnTheManagementPort() {
        Ops.get("/q/health").then().statusCode(200).body("status", equalTo("UP"));
        Ops.get("/q/metrics").then().statusCode(200);
    }

    @Test
    void healthAndMetricsAreNotOnThePublicPort() {
        Ops.get("/q/health").then().statusCode(200);
        Ops.onPublicPort("/q/health").then().statusCode(404);
        Ops.onPublicPort("/q/health/live").then().statusCode(404);
        Ops.onPublicPort("/q/metrics").then().statusCode(404);
    }
}
