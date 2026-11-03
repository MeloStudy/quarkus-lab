package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

/**
 * Task 2: there is one event loop in these tests, as there is one per core in production. Whatever blocks it blocks every
 * request, also the ones that have nothing to do with the slow one.
 */
@QuarkusTest
@TestProfile(ExecutionProfile.class)
class EventLoopTest extends ExecutionTestBase {

    @Test
    void anIdleEventLoopAnswersAtOnce() {
        Load.latency("/ping"); // warm up

        assertThat(Load.latency("/ping"), lessThan(150L));
    }

    @Test
    void slowLookupsDoNotFreezeTheOtherRequests() throws Exception {
        Load.latency("/ping");

        Load.Pending slow = Load.start(3, i -> "/prices/A-" + i);
        Thread.sleep(100); // the three lookups are in progress
        long ping = Load.latency("/ping");
        List<Response> answers = slow.finish();

        assertThat("a ping while three slow lookups are in progress (ms)", ping, lessThan(200L));
        for (Response answer : answers) {
            answer.then().statusCode(200);
        }
    }

    @Test
    void aTotalIsComputedFromTheSlowPrice() {
        Response response = given().get("/prices/A-1/total?quantity=3");

        response.then().statusCode(200);
        assertThat(response.path("sku"), is("A-1"));
        assertThat(response.path("quantity"), is(3));
        assertThat(((Number) response.path("cents")).longValue(), is(3L * 1003));
    }

    @Test
    void totalsDoNotFreezeTheOtherRequestsEither() throws Exception {
        Load.latency("/ping");

        Load.Pending slow = Load.start(3, i -> "/prices/A-" + i + "/total?quantity=2");
        Thread.sleep(100);
        long ping = Load.latency("/ping");
        List<Response> answers = slow.finish();

        assertThat("a ping while three totals are in progress (ms)", ping, lessThan(200L));
        for (Response answer : answers) {
            answer.then().statusCode(200);
        }
    }
}
