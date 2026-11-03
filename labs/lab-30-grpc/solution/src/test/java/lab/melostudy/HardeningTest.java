package lab.melostudy;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import lab.melostudy.grpc.GetProductRequest;
import lab.melostudy.grpc.ListProductsRequest;

/**
 * Task 4: what a service owes to its callers and to itself: no internal details in an error, a key in the metadata, a deadline
 * that the server honours, and a limit on the size of a message.
 */
@QuarkusTest
class HardeningTest extends GrpcTestBase {

    @Test
    void anUnexpectedFailureIsInternalAndLeaksNothing() {
        assertEquals("p1", get("p1").getId());

        assertEquals(Status.Code.INTERNAL, codeOf(() -> get("boom")));
        String description = descriptionOf(() -> get("boom"));
        assertFalse(description.contains("hunter2") || description.contains("db.internal"), "the client was told: " + description);
    }

    @Test
    void aStatusChosenOnPurposeIsNotTurnedIntoInternal() {
        assertEquals(Status.Code.NOT_FOUND, codeOf(() -> get("nope")));
        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(() -> get("")));
    }

    @Test
    void aCallWithoutTheKeyIsUnauthenticated() {
        assertEquals("p1", get("p1").getId());

        assertEquals(Status.Code.UNAUTHENTICATED, codeOf(() -> withoutKey.getProduct(GetProductRequest.newBuilder().setId("p1").build()).await().atMost(WAIT)));
    }

    @Test
    void aCallWithTheWrongKeyIsUnauthenticated() {
        assertEquals("p1", get("p1").getId());

        assertEquals(Status.Code.UNAUTHENTICATED, codeOf(() -> withKey("not-the-key").getProduct(GetProductRequest.newBuilder().setId("p1").build()).await().atMost(WAIT)));
    }

    @Test
    void theKeyIsCheckedOnStreamsToo() {
        assertEquals(2, list(2).size());

        assertEquals(Status.Code.UNAUTHENTICATED,
                codeOf(() -> withoutKey.listProducts(ListProductsRequest.newBuilder().setCount(2).build()).collect().asList().await().atMost(WAIT)));
        assertEquals(Status.Code.UNAUTHENTICATED, codeOf(() -> withoutKey.importProducts(Multi.createFrom().items(product("k1", "No key"))).await().atMost(WAIT)));
    }

    @Test
    void aDeadlineEndsTheCallAndTheServerStopsWorking() {
        Status.Code code = codeOf(() -> catalog().withDeadlineAfter(150, TimeUnit.MILLISECONDS)
                .listProducts(ListProductsRequest.newBuilder().setCount(1000).build()).collect().asList().await().atMost(WAIT));

        assertEquals(Status.Code.DEADLINE_EXCEEDED, code);
        await().atMost(WAIT).until(() -> stats.streamsCancelled() == 1);
        assertTrue(stats.streamed() < 50, "the server kept streaming after the deadline: " + stats.streamed() + " of 1000");
    }

    @Test
    void aMessageOverTheLimitIsRefused() {
        assertEquals(1, importAll(product("ok", "Fine")).getImported());
        String huge = "x".repeat(5 * 1024 * 1024);

        assertEquals(Status.Code.RESOURCE_EXHAUSTED, codeOf(() -> importAll(product("big", huge))));
    }
}
