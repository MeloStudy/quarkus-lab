package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.quarkus.test.junit.QuarkusTest;

/** Task 1: one request, one answer, and the status code says what went wrong. */
@QuarkusTest
class UnaryTest extends GrpcTestBase {

    @Test
    void aKnownProductIsReturned() {
        var product = get("p3");

        assertEquals("p3", product.getId());
        assertEquals("Product 3", product.getName());
        assertEquals(3000, product.getPriceCents());
    }

    @Test
    void anUnknownProductIsNotFound() {
        assertEquals("p1", get("p1").getId());

        assertEquals(Status.Code.NOT_FOUND, codeOf(() -> get("nope")));
    }

    @Test
    void aMissingIdIsAnInvalidArgument() {
        assertEquals("p1", get("p1").getId());

        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(() -> get("")));
        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(() -> get("   ")));
    }
}
