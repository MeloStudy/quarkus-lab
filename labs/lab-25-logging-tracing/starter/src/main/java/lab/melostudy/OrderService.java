package lab.melostudy;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);

    @Inject
    StockGateway gateway;

    @Inject
    OrderStore store;

    @Inject
    AuditLog audit;

    public Order create(OrderRequest request) {
        LOG.infof("Creating an order: %s", request);
        LOG.debugf("Reserving %d units of %s", request.quantity(), request.sku());
        gateway.reserve(request.sku(), request.quantity());
        LOG.tracef("Stock reserved for %s", request.sku());
        Order order = store.add(request.sku(), request.quantity());
        audit.orderCreated(order);
        LOG.infof("Order %d created", order.id());
        return order;
    }
}
