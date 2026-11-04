package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Task 2: the two things the migration guides between 3.33 and 3.40 change without a compile error. Both passed on 3.33; each
 * of them breaks (or exposes a bug) on 3.40 until the code or the configuration is adapted.
 */
@QuarkusTest
class BehaviorAfterUpgradeTest {

    @Test
    void aTransactionalEndpointThatBlocksRunsOnAWorkerThread() {
        String thread = given().get("/tickets/slow").then().statusCode(200).extract().asString();

        org.junit.jupiter.api.Assertions.assertTrue(thread.startsWith("executor-thread"), "the blocking endpoint ran on " + thread);
    }

    @Test
    void aMeterWithTheSameNameCanBeCountedPerRegion() {
        given().get("/tickets/t1").then().statusCode(200);

        given().queryParam("region", "eu").get("/tickets/regional").then().statusCode(200).body(startsWith("counted"));

        // the web counter is still there, and the regional one is really counted (before 3.34 it was silently dropped)
        given().get("/q/metrics").then().statusCode(200).body(containsString("tickets_opened_total{channel=\"web\""))
                .body(containsString("region=\"eu\""));
    }
}
