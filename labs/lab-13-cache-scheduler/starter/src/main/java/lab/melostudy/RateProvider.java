package lab.melostudy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Stands for a slow remote system (a bank API, a legacy database): every {@link #fetch} takes a while and is
 * counted, so tests can tell whether the answer came from the cache or from "upstream".
 */
@ApplicationScoped
public class RateProvider {

    private final Map<String, BigDecimal> rates = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();
    private final Duration latency;

    RateProvider(@ConfigProperty(name = "rates.upstream.latency", defaultValue = "150ms") Duration latency) {
        this.latency = latency;
        seed();
    }

    /** The slow call. Counts itself, even when it fails. */
    public BigDecimal fetch(String currency) {
        calls.incrementAndGet();
        try {
            Thread.sleep(latency.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        BigDecimal rate = rates.get(currency);
        if (rate == null) {
            throw new UnknownCurrencyException(currency);
        }
        return rate;
    }

    /** A write that happens upstream. Does NOT know that a cache exists in front of it. */
    public void update(String currency, BigDecimal rate) {
        rates.put(currency, rate);
    }

    public int calls() {
        return calls.get();
    }

    /** Back to the initial rates and a zero call counter (used by tests). */
    public void reset() {
        rates.clear();
        seed();
        calls.set(0);
    }

    private void seed() {
        rates.put("EUR", new BigDecimal("1.00"));
        rates.put("USD", new BigDecimal("1.10"));
        rates.put("PEN", new BigDecimal("4.10"));
    }
}
