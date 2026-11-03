package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Task 4a: where the handler runs. A consumer that is not marked {@code @Blocking} runs on an event-loop thread, whoever
 * called {@code send()}, and the warehouse driver sleeps.
 */
@QuarkusTest
@Order(4)
class ExecutionTest extends DeskTestBase {

    @Test
    void theWarehouseIsNotCalledOnTheEventLoop() {
        String id = place("A-100", 1).then().statusCode(201).extract().path("id");

        String thread = ledger.find(id).orElseThrow().handledOn();
        assertFalse(thread.contains("eventloop"), "the blocking warehouse call ran on " + thread);
        assertTrue(thread.startsWith("executor-thread"), "expected a worker thread, got " + thread);
    }
}
