package lab.melostudy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

/** What happened to every order that got past the intake step, and on which thread it was handled. */
@ApplicationScoped
public class Ledger {

    public record Entry(String id, String sku, int quantity, OrderStatus status, String handledOn) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public void record(Order order, OrderStatus status) {
        entries.put(order.id(), new Entry(order.id(), order.sku(), order.quantity(), status, Thread.currentThread().getName()));
    }

    public Optional<Entry> find(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    public int size() {
        return entries.size();
    }

    public long count(OrderStatus status) {
        return entries.values().stream().filter(entry -> entry.status() == status).count();
    }

    public void reset() {
        entries.clear();
    }
}
