package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Reactions to OrderPlaced.
 *
 * TODO (Task 5): turn these methods into observers.
 * - shippingPrepared and stockReserved are synchronous; stock must run BEFORE shipping
 *   even though it is declared later (use priority).
 * - customerNotified is asynchronous.
 */
@ApplicationScoped
public class OrderObservers {

    private final EventLog log;

    OrderObservers(EventLog log) {
        this.log = log;
    }

    void shippingPrepared(OrderPlaced event) {
        log.record("shipping:" + event.order().item());
    }

    void stockReserved(OrderPlaced event) {
        log.record("stock:" + event.order().item());
    }

    void customerNotified(OrderPlaced event) {
        log.record("notify:" + event.order().item());
    }
}
