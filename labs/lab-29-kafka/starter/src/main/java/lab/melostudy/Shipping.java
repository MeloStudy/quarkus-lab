package lab.melostudy;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lab.melostudy.Exceptions.UnknownSkuException;

/**
 * Ships the orders that arrive on the {@code orders} topic (consumer group {@code shipping}). Kafka delivers <b>at least
 * once</b>: the same event can come twice (a producer that retries, a consumer that restarts before it committed), so shipping
 * an order must be idempotent. A message the method cannot process throws: what happens next is the failure strategy of the
 * channel (see {@code application.properties}).
 */
@ApplicationScoped
public class Shipping {

    @Inject
    ShipmentStore store;

    // TODO task 4: Kafka delivers at least once. If this order was already shipped, count the duplicate and do nothing else.
    @Incoming("shipping")
    public void ship(Order order) {
        if (!Catalog.SKUS.contains(order.sku())) {
            throw new UnknownSkuException(order.sku());
        }
        store.record(order);
    }
}
