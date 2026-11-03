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
 * Task 3: four worker threads and sixteen requests that each wait 300 ms for a blocking driver. On workers they are served
 * four at a time (four rounds, about 1.2 seconds); on virtual threads all of them wait together.
 */
@QuarkusTest
@TestProfile(ExecutionProfile.class)
class VirtualThreadTest extends ExecutionTestBase {

    @Test
    void aBlockingEndpointCanRunOnAVirtualThread() {
        given().get("/prices/A-1/blocking").then().statusCode(200).body("kind", is("VIRTUAL")).body("cents", is(1003));
    }

    @Test
    void sixteenWaitingRequestsFinishInAboutOneWaitNotFour() {
        given().get("/prices/warm/blocking").then().statusCode(200); // warm up

        Load.Result result = Load.run(16, i -> "/prices/S-" + i + "/blocking");

        for (Response response : result.responses()) {
            response.then().statusCode(200);
        }
        assertThat("sixteen requests, 300 ms each, four workers (ms)", result.millis(), lessThan(800L));
    }
}
