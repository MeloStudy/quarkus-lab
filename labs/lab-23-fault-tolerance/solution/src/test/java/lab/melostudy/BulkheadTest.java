package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 5: one slow carrier must not use up all our threads. At most two requests are inside at the same time. */
@QuarkusTest
class BulkheadTest extends FaultToleranceTestBase {

    @Test
    void onlyTwoRequestsReachTheCarrierAtTheSameTime() throws Exception {
        warmCache("Lima");
        Carrier.delay(400);

        int clients = 6;
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> sources = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            sources.add(pool.submit(() -> {
                start.await();
                return given().get("/api/shipping/Lima").then().statusCode(200).extract().path("source");
            }));
        }
        start.countDown();
        int fresh = 0;
        int cached = 0;
        for (Future<String> source : sources) {
            if ("carrier".equals(source.get())) {
                fresh++;
            } else {
                cached++;
            }
        }
        pool.shutdownNow();

        assertThat("only the two requests that got in were served by the carrier", fresh, equalTo(2));
        assertThat("the others were turned away at once and answered from the cache", cached, equalTo(4));
        assertThat(Carrier.maxConcurrent(), equalTo(2));
    }
}
