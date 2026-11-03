package lab.melostudy;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 4: Kafka delivers at least once, so the same event can arrive twice and must have the effect of arriving once. */
@QuarkusTest
@Order(4)
class IdempotenceTest extends KafkaTestBase {

    @Test
    void theSameEventTwiceIsShippedOnce() {
        String customer = unique("ana");
        String id = UUID.randomUUID().toString();
        String event = orderJson(id, customer, "A-100", 2);

        publish("orders", customer, event);
        publish("orders", customer, event);

        await().atMost(WAIT).until(() -> store.duplicatesOf(id) == 1);
        assertEquals(1, store.timesRecorded(id), "shipped once");
        assertEquals(1, store.duplicatesOf(id), "and the second delivery was recognised");
    }

    @Test
    void aDuplicateDoesNotHideTheOrdersAfterIt() {
        String customer = unique("bob");
        String id = UUID.randomUUID().toString();
        String event = orderJson(id, customer, "B-200", 1);
        publish("orders", customer, event);
        publish("orders", customer, event);

        String next = post(customer, "B-200", 5, null).then().statusCode(202).extract().path("id");

        await().atMost(WAIT).until(() -> store.contains(next));
        assertEquals(1, store.timesRecorded(id));
    }

    @Test
    void auditCountsDeliveriesNotOrders() {
        String customer = unique("cy");
        String event = orderJson(UUID.randomUUID().toString(), customer, "C-300", 1);

        publish("orders", customer, event);
        publish("orders", customer, event);

        await().atMost(WAIT).until(() -> audit.countOf(customer) == 2);
        assertEquals(2, audit.countOf(customer), "audit has no idempotence of its own: it is a different consumer with a different job");
    }
}
