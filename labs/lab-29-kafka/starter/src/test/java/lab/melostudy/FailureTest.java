package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Task 3: an order the consumer cannot process, and a record that is not an order at all, must neither be lost nor stop the
 * consumer. Every test starts by proving that shipping works, so a consumer that never started cannot pass.
 */
@QuarkusTest
@Order(3)
class FailureTest extends KafkaTestBase {

    @BeforeEach
    void shippingWorks() {
        String id = post(unique("warm"), "A-100", 1, null).then().statusCode(202).extract().path("id");
        await().atMost(WAIT).until(() -> store.contains(id));
    }

    private ConsumerRecord<String, String> deadLetterOf(String id) {
        return awaitRecords("orders-dlq", records -> records.stream().anyMatch(r -> r.value().contains(id))).stream().filter(r -> r.value().contains(id)).findFirst().orElseThrow();
    }

    @Test
    void aPoisonOrderEndsUpInTheDeadLetterTopicWithItsReason() {
        String id = UUID.randomUUID().toString();

        publish("orders", unique("poison"), orderJson(id, "someone", "NOPE", 1));

        ConsumerRecord<String, String> dead = deadLetterOf(id);
        assertTrue(dead.value().contains("\"sku\":\"NOPE\""), "the dead letter carries the order: " + dead.value());
        Header reason = dead.headers().lastHeader("dead-letter-reason");
        assertFalse(reason == null || new String(reason.value(), StandardCharsets.UTF_8).isBlank(), "the reason travels in a header");
        assertEquals("orders", new String(dead.headers().lastHeader("dead-letter-topic").value(), StandardCharsets.UTF_8));
    }

    @Test
    void theConsumerKeepsShippingAfterAPoisonOrder() {
        String poisonId = UUID.randomUUID().toString();
        publish("orders", unique("poison"), orderJson(poisonId, "someone", "NOPE", 1));
        deadLetterOf(poisonId);

        String id = post(unique("ana"), "A-100", 1, null).then().statusCode(202).extract().path("id");

        await().atMost(WAIT).until(() -> store.contains(id));
    }

    @Test
    void severalPoisonOrdersInARowDoNotStopIt() {
        List<String> poison = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
        poison.forEach(id -> publish("orders", unique("poison"), orderJson(id, "someone", "NOPE", 1)));
        poison.forEach(this::deadLetterOf);

        String id = post(unique("bob"), "B-200", 1, null).then().statusCode(202).extract().path("id");

        await().atMost(WAIT).until(() -> store.contains(id));
        assertFalse(store.contains(poison.get(0)));
    }

    @Test
    void theServiceStaysReadyAfterAPoisonOrder() {
        String id = UUID.randomUUID().toString();
        publish("orders", unique("poison"), orderJson(id, "someone", "NOPE", 1));
        deadLetterOf(id);

        given().get("/q/health/ready").then().statusCode(200).body("status", is("UP"));
        given().get("/q/health/live").then().statusCode(200).body("status", is("UP"));
    }

    @Test
    void aRecordThatIsNotJsonDoesNotBlockThePartition() {
        String customer = unique("cy");
        String text = "this is not json " + customer;
        publish("orders", customer, text);

        // same key, so the same partition: the good order is right behind the bad record, for both consumer groups
        String id = post(customer, "C-300", 1, null).then().statusCode(202).extract().path("id");

        await().atMost(WAIT).until(() -> store.contains(id));
        await().atMost(WAIT).until(() -> audit.countOf(customer) == 1);
        assertEquals(1, audit.countOf(customer), "audit went past the bad record too");
        ConsumerRecord<String, String> dead = awaitRecords("orders-dlq", records -> records.stream().anyMatch(r -> text.equals(r.value()))).stream()
                .filter(r -> text.equals(r.value())).findFirst().orElseThrow();
        assertEquals(customer, dead.key(), "the dead letter keeps the original bytes and the key");
        assertTrue(dead.headers().lastHeader("deserialization-failure-cause") != null, "and says why it could not be read");
    }
}
