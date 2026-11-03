package lab.melostudy;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

/**
 * Task 4b: a slow consumer and a burst of senders. The channel has room for a few orders; the rest must be told to come back
 * later (503 and {@code Retry-After}) instead of hanging, failing with a 500 or taking the channel down.
 */
@QuarkusTest
@Order(5)
class PressureTest extends DeskTestBase {

    private static final int SENDERS = 40;

    private List<Response> burst() throws Exception {
        ExecutorService senders = Executors.newFixedThreadPool(SENDERS);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Response>> pending = new ArrayList<>();
            for (int i = 0; i < SENDERS; i++) {
                pending.add(senders.submit(() -> {
                    go.await();
                    return place("A-100", 1);
                }));
            }
            go.countDown();
            List<Response> responses = new ArrayList<>();
            for (Future<Response> future : pending) {
                responses.add(future.get());
            }
            return responses;
        } finally {
            senders.shutdownNow();
        }
    }

    @Test
    void aFullChannelAnswers503WithRetryAfter() throws Exception {
        place("A-100", 1).then().statusCode(201);
        ledger.reset();
        warehouse.setDelayMillis(80);

        List<Response> responses = burst();

        long created = responses.stream().filter(r -> r.statusCode() == 201).count();
        List<Response> refused = responses.stream().filter(r -> r.statusCode() == 503).toList();
        assertEquals(SENDERS, created + refused.size(), "every sender got a 201 or a 503, statuses: " + responses.stream().map(Response::statusCode).toList());
        assertFalse(refused.isEmpty(), "with " + SENDERS + " senders and a slow consumer some must be told to wait");
        assertTrue(created >= 1);
        refused.forEach(r -> r.then().header("Retry-After", is("1")).body("code", is("BUSY")));
    }

    @Test
    void everyAcceptedOrderIsFulfilled() throws Exception {
        place("A-100", 1).then().statusCode(201);
        ledger.reset();
        warehouse.setDelayMillis(80);

        long created = burst().stream().filter(r -> r.statusCode() == 201).count();

        assertEquals(created, ledger.count(OrderStatus.FULFILLED), "an order that got a 201 was handled, one that got a 503 was not");
    }

    @Test
    void theChannelRecoversWhenTheBurstIsOver() throws Exception {
        place("A-100", 1).then().statusCode(201);
        warehouse.setDelayMillis(80);
        burst();
        warehouse.setDelayMillis(1);

        place("B-200", 1).then().statusCode(201);
    }
}
