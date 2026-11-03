package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 3: a gRPC method runs on an event-loop thread unless you say otherwise (lab 26), and the repository blocks. */
@QuarkusTest
class ExecutionTest extends GrpcTestBase {

    @Test
    void theBlockingLookupDoesNotRunOnTheEventLoop() {
        assertEquals("p1", get("p1").getId());

        String thread = stats.lastGetProductThread();
        assertFalse(thread.contains("eventloop"), "the blocking repository ran on " + thread);
        assertTrue(thread.startsWith("executor-thread"), "expected a worker thread, got " + thread);
    }
}
