package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Lab 10, task 4: concurrent orders for the last units must neither oversell nor lose updates. */
@QuarkusTest
class ConcurrentOrdersTest {

    @Inject
    TestData data;

    @BeforeEach
    void resetData() {
        data.reset();
    }

    @Test
    void eightBuyersFightForFiveUnits() throws Exception {
        data.setStock(1, 5);
        int buyers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            Callable<Integer> call = () -> {
                go.await();
                return given().contentType(ContentType.JSON).body("{\"productId\":1,\"quantity\":1}")
                        .post("/orders").statusCode();
            };
            results.add(pool.submit(call));
        }
        go.countDown();
        int created = 0;
        int rejected = 0;
        for (Future<Integer> result : results) {
            int status = result.get();
            if (status == 201) {
                created++;
            } else if (status == 409) {
                rejected++;
            }
        }
        pool.shutdown();
        assertThat("accepted", created, equalTo(5));
        assertThat("rejected", rejected, equalTo(buyers - 5));
        assertThat("stock left", data.stockOf(1), equalTo(0));
        assertThat("orders saved", data.count("customer_order"), equalTo(5L));
    }
}
