package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;

/** Task 1: the service publishes the order as JSON, with the customer as the key, and answers once the broker has it. */
@QuarkusTest
@Order(1)
class ProducerTest extends KafkaTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void anAcceptedOrderIsPublishedAsJsonWithItsCustomerAsKey() throws Exception {
        String customer = unique("ana");

        String id = post(customer, "A-100", 2, "gift").then().statusCode(202).extract().path("id");

        ConsumerRecord<String, String> record = awaitRecords("orders", records -> records.stream().anyMatch(r -> r.value().contains(id))).stream()
                .filter(r -> r.value().contains(id)).findFirst().orElseThrow();
        assertEquals(customer, record.key(), "the key of the record is the customer");
        JsonNode value = JSON.readTree(record.value());
        assertEquals(id, value.get("id").asText());
        assertEquals(customer, value.get("customer").asText());
        assertEquals("A-100", value.get("sku").asText());
        assertEquals(2, value.get("quantity").asInt());
    }

    @Test
    void ordersOfTheSameCustomerShareAPartitionAndKeepTheirOrder() {
        String customer = unique("bob");
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ids.add(post(customer, "B-200", i, null).then().statusCode(202).extract().path("id"));
        }

        List<ConsumerRecord<String, String>> mine = awaitRecords("orders", records -> records.stream().filter(r -> customer.equals(r.key())).count() == 5).stream()
                .filter(r -> customer.equals(r.key())).toList();
        assertEquals(1, mine.stream().map(ConsumerRecord::partition).distinct().count(), "one customer, one partition");
        List<String> idsInTheTopic = mine.stream().sorted((a, b) -> Long.compare(a.offset(), b.offset())).map(r -> {
            try {
                return JSON.readTree(r.value()).get("id").asText();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
        assertEquals(ids, idsInTheTopic, "the partition keeps the order in which the orders were placed");
    }

    @Test
    void anInvalidOrderIsRefusedAndNeverPublished() {
        String good = unique("cy");
        String bad = unique("dee");
        String goodId = post(good, "C-300", 1, null).then().statusCode(202).extract().path("id");

        post(bad, "NOPE", 1, null).then().statusCode(422).body("code", is("INVALID_ORDER"));
        post(bad, "A-100", 0, null).then().statusCode(422).body("code", is("INVALID_ORDER"));

        List<ConsumerRecord<String, String>> records = awaitRecords("orders", all -> all.stream().anyMatch(r -> r.value().contains(goodId)));
        assertTrue(records.stream().noneMatch(r -> bad.equals(r.key())), "an order that was refused must not be in the topic");
    }

    @Test
    void aRecordTheBrokerRefusesIsNotAccepted() {
        post(unique("eve"), "A-100", 1, null).then().statusCode(202);

        // larger than max.request.size (1 MiB by default): the client never sends it, the stage fails
        post(unique("eve"), "A-100", 1, "x".repeat(1_200_000)).then().statusCode(502).body("code", is("BROKER_REJECTED"));
    }

    @Test
    void theDoorKeepsWorkingAfterARefusedRecord() {
        post(unique("fay"), "A-100", 1, "x".repeat(1_200_000)).then().statusCode(502);

        given().contentType("application/json").body("{\"customer\":\"" + unique("fay") + "\",\"sku\":\"A-100\",\"quantity\":1}").post("/orders").then().statusCode(202);
    }
}
