package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 2: a transient failure is retried; a mistake of the caller is not. */
@QuarkusTest
class RetryTest extends FaultToleranceTestBase {

    @Test
    void oneFailureIsAbsorbed() {
        Carrier.failNext(1, 503);
        given().get("/api/shipping/Lima").then().statusCode(200).body("source", equalTo("carrier"));
        assertThat(Carrier.calls(), equalTo(2));
    }

    @Test
    void twoFailuresAreAbsorbed() {
        Carrier.failNext(2, 503);
        given().get("/api/shipping/Lima").then().statusCode(200).body("source", equalTo("carrier"));
        assertThat(Carrier.calls(), equalTo(3));
    }

    @Test
    void retriesStopAfterTheLimit() {
        Carrier.failNext(3, 503);
        given().get("/api/shipping/Quito").then().statusCode(503);
        assertThat("1 attempt + 2 retries", Carrier.calls(), equalTo(3));
    }

    @Test
    void anUnknownCityIsNotRetried() {
        given().get("/api/shipping/Lima").then().statusCode(200);
        Carrier.reset();
        given().get("/api/shipping/Atlantis").then().statusCode(404).body("code", equalTo("UNKNOWN_CITY"));
        assertThat("a 404 cannot be fixed by asking again", Carrier.calls(), equalTo(1));
    }
}
