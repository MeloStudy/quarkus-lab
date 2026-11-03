package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The first stage of the pipeline: a processor between the {@code orders} and the {@code fulfilment} channels. It only looks
 * at the order itself, so it is cheap and never blocks.
 */
@ApplicationScoped
public class OrderIntake {

    // TODO task 1: make this method the processor between "orders" and "fulfilment", and refuse a bad order by throwing
    //   InvalidOrderException: a quantity that is not positive, and a sku that is not in Warehouse.CATALOG.
    public Order validate(Order order) {
        return order;
    }
}
