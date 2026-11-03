package lab.melostudy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderStore {

    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public Order add(String sku, int quantity) {
        long id = ids.incrementAndGet();
        Order order = new Order(id, sku, quantity);
        orders.put(id, order);
        return order;
    }

    public Optional<Order> find(long id) {
        return Optional.ofNullable(orders.get(id));
    }
}
