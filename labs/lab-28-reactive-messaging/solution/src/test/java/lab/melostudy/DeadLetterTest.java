package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Task 3: a failure that can go away is retried, and when it does not go away the order is kept, not lost. A failure that is
 * the sender's (or the business's) is neither retried nor kept.
 */
@QuarkusTest
@Order(3)
class DeadLetterTest extends DeskTestBase {

    @BeforeEach
    void thePipelineWorks() {
        place("A-100", 1).then().statusCode(201);
        ledger.reset();
        warehouse.reset();
    }

    @Test
    void aTransientFailureIsRetriedAndTheOrderGoesThrough() {
        warehouse.failNext(2);

        place("A-100", 1).then().statusCode(201).body("status", is("FULFILLED"));

        assertEquals(3, warehouse.calls(), "two failures and a success");
        assertTrue(deadLetters.all().isEmpty());
    }

    @Test
    void aFailureThatNeverGoesAwayAnswers502() {
        warehouse.failNext(10);

        place("A-100", 1).then().statusCode(502).body("code", is("WAREHOUSE_UNAVAILABLE"));

        assertEquals(3, warehouse.calls(), "the call and its two retries");
    }

    @Test
    void theOrderIsKeptInTheDeadLetters() {
        warehouse.failNext(10);

        place("B-200", 4).then().statusCode(502);

        assertEquals(1, deadLetters.all().size());
        DeadLetter letter = deadLetters.all().get(0);
        assertEquals("B-200", letter.order().sku());
        assertEquals(4, letter.order().quantity());
        assertFalse(letter.reason().isBlank());
        assertEquals(OrderStatus.DEAD, ledger.find(letter.order().id()).orElseThrow().status());
    }

    @Test
    void aWarehouseThatIsAlwaysOfflineGoesToTheDeadLetters() {
        place(Warehouse.OFFLINE, 1).then().statusCode(502).body("code", is("WAREHOUSE_UNAVAILABLE"));

        assertEquals(1, deadLetters.all().size());
    }

    @Test
    void anOperatorCanReadTheDeadLetters() {
        warehouse.failNext(10);
        place("C-300", 2).then().statusCode(502);

        given().get("/dead-letters").then().statusCode(200).body("$", hasSize(1)).body("[0].order.sku", is("C-300"));
    }

    @Test
    void anOutOfStockOrderIsNeitherRetriedNorKept() {
        place(Warehouse.SOLD_OUT, 1).then().statusCode(409);

        assertEquals(1, warehouse.calls(), "no retry for a business answer");
        assertTrue(deadLetters.all().isEmpty());
    }

    @Test
    void aRefusedOrderIsNotKept() {
        place("Z-999", 1).then().statusCode(422);

        assertTrue(deadLetters.all().isEmpty());
    }

    @Test
    void theChannelKeepsWorkingAfterADeadLetter() {
        warehouse.failNext(10);
        place("A-100", 1).then().statusCode(502);
        warehouse.failNext(0);

        place("A-100", 1).then().statusCode(201);
        assertEquals(1, deadLetters.all().size());
    }
}
