package lab.melostudy;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 2: two consumer groups read the same topic; each of them gets every order, and a customer's orders arrive in order. */
@QuarkusTest
@Order(2)
class ConsumerTest extends KafkaTestBase {

    @Test
    void shippingShipsWhatTheDoorPublished() {
        String customer = unique("ana");

        String id = post(customer, "A-100", 3, null).then().statusCode(202).extract().path("id");

        await().atMost(WAIT).until(() -> store.contains(id));
        assertEquals(customer, store.find(id).orElseThrow().customer());
        assertEquals(3, store.find(id).orElseThrow().quantity());
    }

    @Test
    void auditReadsEveryOrderToo() {
        String customer = unique("bob");
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ids.add(post(customer, "B-200", 1, null).then().statusCode(202).extract().path("id"));
        }

        await().atMost(WAIT).until(() -> ids.stream().allMatch(store::contains));
        await().atMost(WAIT).until(() -> audit.countOf(customer) == 4);
        assertEquals(4, audit.countOf(customer), "another consumer group gets its own copy of every order");
    }

    @Test
    void theOrdersOfOneCustomerAreShippedInTheOrderTheyWerePlaced() {
        String customer = unique("cy");
        for (int quantity = 1; quantity <= 8; quantity++) {
            post(customer, "C-300", quantity, null).then().statusCode(202);
        }

        await().atMost(WAIT).pollInterval(Duration.ofMillis(200)).until(() -> store.arrivalsOf(customer).size() == 8);
        List<Integer> quantities = store.arrivalsOf(customer).stream().map(order -> order.quantity()).toList();
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), quantities);
    }
}
