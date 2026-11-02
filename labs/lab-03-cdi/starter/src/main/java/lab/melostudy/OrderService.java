package lab.melostudy;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Places orders.
 *
 * TODO (Task 3): the time must come from an injected java.time.Clock, not from Instant.now().
 * TODO (Task 4): audit place() with an interceptor.
 * TODO (Task 5): fire OrderPlaced (synchronous and asynchronous) when the order is placed.
 */
@ApplicationScoped
public class OrderService {

    private final AtomicLong sequence = new AtomicLong();

    public Order place(String item, int quantity) {
        return new Order(sequence.incrementAndGet(), item, quantity, Instant.now());
    }
}
