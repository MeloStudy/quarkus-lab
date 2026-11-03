package lab.melostudy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Stands for a remote inventory service: this lab is about how the application reports on itself, not about
 * calling it. The tests switch it off to see what the health checks say.
 */
@ApplicationScoped
public class InventoryGateway {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of("A-100", 1_000_000, "B-200", 5));
    private volatile boolean available = true;

    public synchronized void reserve(String sku, int quantity) {
        if (!available) {
            throw new InventoryUnavailableException();
        }
        Integer current = stock.get(sku);
        if (current == null || current < quantity) {
            throw new OutOfStockException(sku);
        }
        stock.put(sku, current - quantity);
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
