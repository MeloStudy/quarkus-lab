package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

/**
 * Task 4: the endpoint already runs on virtual threads, and still it is slow. The tests give the virtual threads two carrier
 * threads (see the {@code pom.xml}); on a server with many cores the same code would only collapse at higher load.
 */
@QuarkusTest
@TestProfile(ExecutionProfile.class)
class PinningTest extends ExecutionTestBase {

    @Test
    void sixteenDifferentSkusThroughTheCacheFinishInAboutOneWait() {
        given().get("/prices/warm/blocking").then().statusCode(200); // warm up

        Load.Result result = Load.run(16, i -> "/prices/C-" + i + "/cached");

        for (Response response : result.responses()) {
            response.then().statusCode(200).body("kind", is("VIRTUAL"));
        }
        assertThat("sixteen different SKUs, 300 ms each, two carrier threads (ms)", result.millis(), lessThan(800L));
    }

    @Test
    void theSameSkuAskedFourTimesAtOnceIsLookedUpOnce() {
        Load.Result result = Load.run(4, i -> "/prices/SAME/cached");

        for (Response response : result.responses()) {
            response.then().statusCode(200).body("cents", is(1004));
        }
        assertThat("the driver was called once", given().get("/driver/calls").asString(), is("1"));
    }

    @Test
    void aCachedPriceIsServedWithoutTheDriver() {
        given().get("/prices/KEPT/cached").then().statusCode(200);

        long start = System.nanoTime();
        given().get("/prices/KEPT/cached").then().statusCode(200);
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertThat("the second answer comes from the cache (ms)", millis, lessThan(150L));
        assertThat(given().get("/driver/calls").asString(), is("1"));
    }
}
