package lab.inventory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

/** The stock of every product, in memory. Starts with two products: A1 (5 units) and B2 (sold out). */
@ApplicationScoped
public class StockRepository {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();

    StockRepository() {
        reset();
    }

    public Optional<Integer> available(String sku) {
        return Optional.ofNullable(stock.get(sku));
    }

    /** Takes {@code quantity} units if there are enough; returns the units left, or empty if there are not enough. */
    public synchronized Optional<Integer> take(String sku, int quantity) {
        int current = stock.getOrDefault(sku, 0);
        if (current < quantity) {
            return Optional.empty();
        }
        stock.put(sku, current - quantity);
        return Optional.of(current - quantity);
    }

    public void set(String sku, int quantity) {
        stock.put(sku, quantity);
    }

    /** Back to the initial state (used by tests to set up the state a contract interaction needs). */
    public void reset() {
        stock.clear();
        stock.put("A1", 5);
        stock.put("B2", 0);
    }
}
