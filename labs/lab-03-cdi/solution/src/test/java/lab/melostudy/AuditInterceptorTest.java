package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AuditInterceptorTest {

    @Inject
    OrderService orders;

    @Inject
    AuditLog audit;

    @BeforeEach
    void reset() {
        audit.clear();
    }

    @Test
    void placingAnOrderIsAudited() {
        orders.place("monitor", 2);
        assertEquals(1, audit.entries().size());
        assertTrue(audit.entries().get(0).contains("OrderService.place"),
                "Entrada de auditoría inesperada: " + audit.entries());
    }

    @Test
    void auditedMethodStillReturnsItsResult() {
        Order order = orders.place("ratón", 3);
        assertEquals("ratón", order.item());
        assertEquals(3, order.quantity());
    }
}
