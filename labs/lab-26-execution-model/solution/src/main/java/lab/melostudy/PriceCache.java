package lab.melostudy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Remembers prices so that the slow driver is asked once per SKU. One lock per SKU: two requests for different SKUs never wait
 * for each other, two requests for the same SKU make one call.
 * <p>
 * It is called from virtual threads, and the lock is held <b>while the driver blocks</b>. On Java 21 a virtual thread that
 * blocks inside a {@code synchronized} block keeps its carrier thread (it is "pinned"); a {@link ReentrantLock} lets it go.
 */
@ApplicationScoped
public class PriceCache {

    private final Map<String, Price> prices = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Inject
    LegacyCatalog catalog;

    public Price get(String sku) {
        ReentrantLock lock = locks.computeIfAbsent(sku, key -> new ReentrantLock());
        lock.lock();
        try {
            Price known = prices.get(sku);
            if (known == null) {
                known = catalog.lookup(sku);
                prices.put(sku, known);
            }
            return known;
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        prices.clear();
        locks.clear();
    }
}
