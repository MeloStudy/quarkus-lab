package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ClockProducerTest {

    @Inject
    OrderService orders;

    @Test
    void orderTimestampComesFromTheInjectedClock() {
        Order order = orders.place("teclado", 1);
        assertEquals(FixedClockProducer.NOW, order.createdAt());
    }
}
