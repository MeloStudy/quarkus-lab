package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/** Task 1 (the table): a {@code Uni} describes work, it does not do it; and every subscription does it again. */
class LazinessTest {

    private static final Duration LIMIT = Duration.ofSeconds(3);

    /** Counts how many times it really loads, and on which thread. */
    private static final class CountingLoader implements RateLoader {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<Thread> thread = new AtomicReference<>();

        @Override
        public Map<String, Double> load() {
            calls.incrementAndGet();
            thread.set(Thread.currentThread());
            return Map.of("EUR", 1.0, "USD", 1.08);
        }
    }

    @Test
    void nothingIsLoadedUntilSomebodyAsksForTheRates() {
        CountingLoader loader = new CountingLoader();

        RateTable table = new RateTable(loader);

        assertEquals(0, loader.calls.get(), "building the table must not load anything");
        table.rates().await().atMost(LIMIT);
        assertEquals(1, loader.calls.get());
    }

    @Test
    void manyAskersShareOneLoad() {
        CountingLoader loader = new CountingLoader();
        RateTable table = new RateTable(loader);

        for (int i = 0; i < 5; i++) {
            assertEquals(1.08, table.rates().await().atMost(LIMIT).get("USD"));
        }

        assertEquals(1, loader.calls.get(), "five subscriptions, one load");
    }

    @Test
    void theBlockingLoaderDoesNotRunOnTheThreadThatAsks() {
        CountingLoader loader = new CountingLoader();
        RateTable table = new RateTable(loader);

        table.rates().await().atMost(LIMIT);

        assertNotSame(Thread.currentThread(), loader.thread.get(), "the loader blocks: it must run on a worker thread");
    }
}
