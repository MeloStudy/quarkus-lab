package lab.melostudy;

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

    public Order create(OrderRequest request) {
        inventory.reserve(request.sku(), request.quantity());
        String type = request.type() == null ? "standard" : request.type();
        Order order = store.add(request.sku(), request.quantity(), type);
        processor.submit(order.id());
        // TODO task 3: count the created orders, one counter named orders.created with the tag type
        return order;
    }
}
