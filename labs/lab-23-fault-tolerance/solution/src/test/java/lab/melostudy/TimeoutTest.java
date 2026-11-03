package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 1: a slow carrier must not make our API slow. */
@QuarkusTest
class TimeoutTest extends FaultToleranceTestBase {

    @Test
    void healthyCarrierAnswersWithAFreshPrice() {
        given().get("/api/shipping/Lima").then()
                .statusCode(200)
                .body("price", equalTo(12.5f))
                .body("source", equalTo("carrier"));
    }

    @Test
    void slowCarrierIsCutOffQuickly() {
        given().get("/api/shipping/Lima").then().statusCode(200);
        shipping.clearCache();
        Carrier.delay(3000);

        long start = System.nanoTime();
        int status = given().get("/api/shipping/Lima").then().extract().statusCode();
        long millis = (System.nanoTime() - start) / 1_000_000;

        // the stub needs 3000 ms and the client read timeout is 5000 ms: only our own timeout can cut it short
        assertThat(millis, lessThan(2500L));
        assertThat(status, anyOf(equalTo(503), equalTo(504)));
    }
}
