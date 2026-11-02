package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Lab 07, task 3: a slow dependency must not make our API slow forever. */
@QuarkusTest
@QuarkusTestResource(RatesStubResource.class)
class TimeoutTest {

    @Inject
    TestData data;

    @BeforeEach
    void resetData() {
        data.reset();
    }


    @Test
    void slowRemoteBecomesGatewayTimeoutQuickly() {
        long start = System.nanoTime();
        given().queryParam("currency", "SLO")
                .when().get("/products/1/price")
                .then().statusCode(504)
                .body("code", equalTo("UPSTREAM_TIMEOUT"));
        long millis = (System.nanoTime() - start) / 1_000_000;
        // the stub needs 3000 ms; our read timeout in the test profile is 500 ms
        assertThat(millis, lessThan(2500L));
    }
}
