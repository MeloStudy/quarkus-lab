package lab.melostudy;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lab.melostudy.Exceptions.OutOfStockException;
import lab.melostudy.Exceptions.WarehouseUnavailableException;

/**
 * A slow, blocking warehouse driver (think JDBC or a legacy SOAP call). Not the topic of the lab: it is the thing that fails
 * in the ways the messaging code has to survive. The retry belongs here, to the call that fails, and only for the failure that
 * can go away (lab 23).
 */
@ApplicationScoped
public class Warehouse {

    /** Known skus. {@code SOLD-OUT} has no stock (the "poison" order of the lab); {@code OFFLINE} lives in a warehouse that never answers. */
    public static final String SOLD_OUT = "SOLD-OUT";
    public static final String OFFLINE = "OFFLINE";
    public static final Set<String> CATALOG = Set.of("A-100", "B-200", "C-300", SOLD_OUT, OFFLINE);

    private final long configuredDelayMillis;
    private volatile long delayMillis;
    private final AtomicInteger failNext = new AtomicInteger();
    private final AtomicInteger calls = new AtomicInteger();

    @Inject
    public Warehouse(@ConfigProperty(name = "app.warehouse.delay-millis", defaultValue = "20") long delayMillis) {
        this.configuredDelayMillis = delayMillis;
        this.delayMillis = delayMillis;
    }

    @Retry(maxRetries = 2, delay = 10, retryOn = WarehouseUnavailableException.class)
    public void reserve(Order order) {
        calls.incrementAndGet();
        sleep(delayMillis);
        if (OFFLINE.equals(order.sku()) || failNext.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
            throw new WarehouseUnavailableException("The warehouse did not answer for " + order.id());
        }
        if (SOLD_OUT.equals(order.sku())) {
            throw new OutOfStockException(order.sku());
        }
    }

    /** How many times the driver was called, retries included. */
    public int calls() {
        return calls.get();
    }

    /** The next {@code n} calls fail as "unavailable". */
    public void failNext(int n) {
        failNext.set(n);
    }

    public void setDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public void reset() {
        failNext.set(0);
        calls.set(0);
        delayMillis = configuredDelayMillis;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
