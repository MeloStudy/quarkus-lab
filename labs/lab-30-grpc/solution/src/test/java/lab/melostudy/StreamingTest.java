package lab.melostudy;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import lab.melostudy.grpc.ListProductsRequest;
import lab.melostudy.grpc.Product;

/** Task 2: many answers over time, many requests before one answer, and what happens when somebody stops listening. */
@QuarkusTest
class StreamingTest extends GrpcTestBase {

    @Test
    void aServerStreamDeliversEveryProductInOrder() {
        List<Product> products = list(5);

        assertEquals(5, products.size());
        assertEquals(List.of("n0", "n1", "n2", "n3", "n4"), products.stream().map(Product::getId).toList());
    }

    @Test
    void aCountOutsideTheLimitsIsAnInvalidArgument() {
        assertEquals(2, list(2).size());

        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(() -> list(0)));
        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(() -> list(1001)));
    }

    @Test
    void aClientThatStopsListeningStopsTheServerToo() {
        List<Product> few = catalog().listProducts(ListProductsRequest.newBuilder().setCount(1000).build()).select().first(3).collect().asList().await().atMost(WAIT);

        assertEquals(3, few.size());
        await().atMost(WAIT).until(() -> stats.streamsCancelled() == 1);
        assertTrue(stats.streamed() < 20, "the server kept producing after the client left: " + stats.streamed() + " products of 1000");
    }

    @Test
    void importedProductsCanBeRead() {
        assertEquals(3, importAll(product("x1", "One"), product("x2", "Two"), product("x3", "Three")).getImported());

        assertEquals("Two", get("x2").getName());
    }

    @Test
    void anImportWithABadProductIsRefusedAndStoresNothing() {
        assertEquals(1, importAll(product("ok", "Fine")).getImported());

        Status.Code code = codeOf(() -> catalog().importProducts(Multi.createFrom().items(product("a1", "Good"), product("a2", ""), product("a3", "Good"))).await().atMost(WAIT));

        assertEquals(Status.Code.INVALID_ARGUMENT, code);
        assertEquals(Status.Code.NOT_FOUND, codeOf(() -> get("a1")), "all or nothing: the product before the bad one was not stored");
        assertEquals(Status.Code.NOT_FOUND, codeOf(() -> get("a3")));
    }

    @Test
    void anEmptyImportImportsNothing() {
        assertEquals(1, importAll(product("ok", "Fine")).getImported());

        assertEquals(0, importAll().getImported());
    }
}
