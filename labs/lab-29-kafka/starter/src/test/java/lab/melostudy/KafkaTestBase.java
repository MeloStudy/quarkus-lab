package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.inject.Inject;

/**
 * Shared by the tests. All the test classes use one application and one broker (Dev Services), and the topics keep what was
 * published in earlier tests: every test works with ids and customers of its own and looks only at those.
 */
@QuarkusTestResource(KafkaCompanionResource.class)
abstract class KafkaTestBase {

    static final Duration WAIT = Duration.ofSeconds(30);

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    ShipmentStore store;

    @Inject
    Audit audit;

    static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static Response post(String customer, String sku, int quantity, String note) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("customer", customer);
        body.put("sku", sku);
        body.put("quantity", quantity);
        body.put("note", note);
        return given().contentType(ContentType.JSON).body(body).post("/orders");
    }

    /** An order as another producer (not this service) would write it. */
    static String orderJson(String id, String customer, String sku, int quantity) {
        return "{\"id\":\"" + id + "\",\"customer\":\"" + customer + "\",\"sku\":\"" + sku + "\",\"quantity\":" + quantity + ",\"note\":null}";
    }

    /** Publishes a record straight to a topic, the way another service would. */
    void publish(String topic, String key, String value) {
        companion.produceStrings().fromRecords(new ProducerRecord<>(topic, key, value)).awaitCompletion();
    }

    /** Everything that is in a topic right now, read from the beginning by a consumer group of its own. */
    List<ConsumerRecord<String, String>> readAll(String topic) {
        return companion.consumeStrings().withGroupId(UUID.randomUUID().toString()).fromTopics(topic, Duration.ofSeconds(2)).awaitCompletion().getRecords();
    }

    /** Waits until a record of the topic satisfies the condition, and returns the records that were read. */
    List<ConsumerRecord<String, String>> awaitRecords(String topic, Predicate<List<ConsumerRecord<String, String>>> condition) {
        List<ConsumerRecord<String, String>>[] last = new List[1];
        await().atMost(WAIT).pollInterval(Duration.ofMillis(200)).until(() -> {
            last[0] = readAll(topic);
            return condition.test(last[0]);
        });
        return last[0];
    }
}
