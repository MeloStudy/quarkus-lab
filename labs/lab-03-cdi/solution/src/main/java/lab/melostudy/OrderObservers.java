package lab.melostudy;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

/** Reactions to OrderPlaced. */
@ApplicationScoped
public class OrderObservers {

    private final EventLog log;

    OrderObservers(EventLog log) {
        this.log = log;
    }

    // Lower priority number = runs earlier. Declared first on purpose.
    void shippingPrepared(@Observes @Priority(2000) OrderPlaced event) {
        log.record("shipping:" + event.order().item());
    }

    void stockReserved(@Observes @Priority(1000) OrderPlaced event) {
        log.record("stock:" + event.order().item());
    }

    void customerNotified(@ObservesAsync OrderPlaced event) {
        log.record("notify:" + event.order().item());
    }
}
