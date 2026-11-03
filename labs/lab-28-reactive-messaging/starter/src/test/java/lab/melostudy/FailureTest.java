package lab.melostudy;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Task 2: an order that cannot be fulfilled must tell its sender, and must not stop the orders behind it. Every test starts
 * from an order that is known to work, so a broken pipeline cannot pass by answering something else.
 */
@QuarkusTest
@Order(2)
class FailureTest extends DeskTestBase {

    @BeforeEach
    void thePipelineWorks() {
        place("A-100", 1).then().statusCode(201);
        ledger.reset();
        warehouse.reset();
    }

    @Test
    void anOutOfStockOrderIsRefusedWith409() {
        place(Warehouse.SOLD_OUT, 1).then().statusCode(409).body("code", is("OUT_OF_STOCK"));
    }

    @Test
    void theLedgerRecordsWhatHappened() {
        place(Warehouse.SOLD_OUT, 1).then().statusCode(409);

        assertEquals(1, ledger.count(OrderStatus.OUT_OF_STOCK));
        assertEquals(0, ledger.count(OrderStatus.FULFILLED));
    }

    @Test
    void theChannelKeepsWorkingAfterAFailedOrder() {
        place(Warehouse.SOLD_OUT, 1).then().statusCode(409);

        place("A-100", 1).then().statusCode(201).body("status", is("FULFILLED"));
    }

    @Test
    void theChannelSurvivesSeveralFailures() {
        for (int i = 0; i < 3; i++) {
            place(Warehouse.SOLD_OUT, 1).then().statusCode(409);
        }

        place("B-200", 2).then().statusCode(201);
        place("C-300", 3).then().statusCode(201);
        assertEquals(2, ledger.count(OrderStatus.FULFILLED));
    }
}
