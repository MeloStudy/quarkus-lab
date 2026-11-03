package lab.melostudy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

/** What the shipping consumer has done: the orders shipped, in the order they arrived, and the duplicates it met. */
@ApplicationScoped
public class ShipmentStore {

    private final Map<String, Order> byId = new ConcurrentHashMap<>();
    private final List<Order> arrivals = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicInteger> duplicates = new ConcurrentHashMap<>();

    public boolean contains(String id) {
        return byId.containsKey(id);
    }

    public void record(Order order) {
        byId.put(order.id(), order);
        arrivals.add(order);
    }

    public void countDuplicate(String id) {
        duplicates.computeIfAbsent(id, key -> new AtomicInteger()).incrementAndGet();
    }

    public Optional<Order> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** How many times the order with this id was recorded (it must be 0 or 1). */
    public long timesRecorded(String id) {
        return arrivals.stream().filter(order -> order.id().equals(id)).count();
    }

    public int duplicatesOf(String id) {
        AtomicInteger count = duplicates.get(id);
        return count == null ? 0 : count.get();
    }

    /** The orders of one customer, in the order the consumer saw them. */
    public List<Order> arrivalsOf(String customer) {
        return arrivals.stream().filter(order -> customer.equals(order.customer())).toList();
    }

    public int size() {
        return byId.size();
    }
}
