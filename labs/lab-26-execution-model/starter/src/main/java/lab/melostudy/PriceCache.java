package lab.melostudy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Remembers prices so that the slow driver is asked once per SKU. One lock per SKU: two requests for different SKUs never wait
 * for each other, two requests for the same SKU make one call.
 * <p>
 * TODO Task 4: it is called from virtual threads, and the lock is held while the driver blocks.
 */
@ApplicationScoped
public class PriceCache {

    private final Map<String, Price> prices = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    @Inject
    LegacyCatalog catalog;

    public Price get(String sku) {
        Object lock = locks.computeIfAbsent(sku, key -> new Object());
        synchronized (lock) {
            Price known = prices.get(sku);
            if (known == null) {
                known = catalog.lookup(sku);
                prices.put(sku, known);
            }
            return known;
        }
    }

    public void clear() {
        prices.clear();
        locks.clear();
    }
}
