package lab.melostudy;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;

/** Registra pedidos. */
@ApplicationScoped
public class OrderService {

    private final AtomicLong sequence = new AtomicLong();
    private final Clock clock;
    private final Event<OrderPlaced> orderPlaced;

    OrderService(Clock clock, Event<OrderPlaced> orderPlaced) {
        this.clock = clock;
        this.orderPlaced = orderPlaced;
    }

    @Audited
    public Order place(String item, int quantity) {
        Order order = new Order(sequence.incrementAndGet(), item, quantity, Instant.now(clock));
        OrderPlaced event = new OrderPlaced(order);
        orderPlaced.fire(event);
        orderPlaced.fireAsync(event);
        return order;
    }
}
