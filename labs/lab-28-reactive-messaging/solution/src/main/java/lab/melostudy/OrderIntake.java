package lab.melostudy;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import jakarta.enterprise.context.ApplicationScoped;
import lab.melostudy.Exceptions.InvalidOrderException;

/**
 * The first stage of the pipeline: a processor between {@code orders} and {@code fulfilment}. It only looks at the order
 * itself, so it is cheap and never blocks.
 */
@ApplicationScoped
public class OrderIntake {

    @Incoming("orders")
    @Outgoing("fulfilment")
    public Order validate(Order order) {
        if (order.quantity() <= 0) {
            throw new InvalidOrderException("The quantity must be positive");
        }
        if (order.sku() == null || !Warehouse.CATALOG.contains(order.sku())) {
            throw new InvalidOrderException("Unknown sku " + order.sku());
        }
        return order;
    }
}
