package lab.melostudy;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 2: liveness, readiness and startup answer three different questions. */
@QuarkusTest
class HealthTest extends OpsTestBase {

    @Test
    void healthyApplicationIsUpEverywhere() {
        Ops.get("/q/health/live").then().statusCode(200).body("status", equalTo("UP")).body("checks.name", contains("order-worker"));
        Ops.get("/q/health/ready").then().statusCode(200).body("status", equalTo("UP")).body("checks.name", hasItem("inventory"));
        Ops.get("/q/health/started").then().statusCode(200).body("status", equalTo("UP")).body("checks.name", contains("worker-started"));
    }

    @Test
    void withoutTheInventoryTheApplicationIsNotReady() {
        Ops.get("/q/health/ready").then().statusCode(200);
        inventory.setAvailable(false);
        Ops.get("/q/health/ready").then()
                .statusCode(503)
                .body("status", equalTo("DOWN"))
                .body("checks.find { it.name == 'inventory' }.status", equalTo("DOWN"));
    }

    @Test
    void withoutTheInventoryTheApplicationIsStillAlive() {
        Ops.get("/q/health/live").then().statusCode(200);
        inventory.setAvailable(false);
        Ops.get("/q/health/live").then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    void aStuckWorkerIsNotAlive() throws InterruptedException {
        Ops.get("/q/health/live").then().statusCode(200);
        processor.stall(true);
        Thread.sleep(1500); // the test profile accepts one second of silence
        Ops.get("/q/health/live").then()
                .statusCode(503)
                .body("status", equalTo("DOWN"))
                .body("checks.find { it.name == 'order-worker' }.status", equalTo("DOWN"));

        processor.stall(false);
        Thread.sleep(300);
        Ops.get("/q/health/live").then().statusCode(200);
    }

    @Test
    void theOverallEndpointCombinesEverything() {
        Ops.get("/q/health").then().statusCode(200).body("status", equalTo("UP"));
        inventory.setAvailable(false);
        Ops.get("/q/health").then().statusCode(503).body("status", equalTo("DOWN"));
    }
}
