package lab.melostudy;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Stands in for a blocking driver (JDBC, an old SOAP client, a file share): the thread that calls it waits. The wait is
 * {@code app.legacy.latency-millis} (300 ms) and it is the same wherever it runs.
 */
@ApplicationScoped
public class LegacyCatalog {

    private final AtomicInteger calls = new AtomicInteger();

    @ConfigProperty(name = "app.legacy.latency-millis", defaultValue = "300")
    int latencyMillis;

    public Price lookup(String sku) {
        calls.incrementAndGet();
        try {
            Thread.sleep(latencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new Price(sku, 1000 + sku.length());
    }

    /** How many times the driver was really called. */
    public int calls() {
        return calls.get();
    }

    public void resetCalls() {
        calls.set(0);
    }
}
