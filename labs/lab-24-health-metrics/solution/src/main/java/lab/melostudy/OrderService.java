package lab.melostudy;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderService {

    @Inject
    InventoryGateway inventory;

    @Inject
    OrderStore store;

    @Inject
    OrderProcessor processor;

    @Inject
    MeterRegistry registry;

    public Order create(OrderRequest request) {
        inventory.reserve(request.sku(), request.quantity());
        String type = request.type() == null ? "standard" : request.type();
        Order order = store.add(request.sku(), request.quantity(), type);
        processor.submit(order.id());
        registry.counter("orders.created", "type", type).increment();
        return order;
    }
}
