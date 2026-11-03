package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Task 2: three sources that answer in 200 ms each. Asked one after another that is 600 ms; asked together, about 200. */
class CompositionTest {

    private static final Duration LIMIT = Duration.ofSeconds(3);

    @Test
    void theCheapestQuoteWins() {
        QuoteService service = new QuoteService(List.of(Probe.fixed("a", 120, 0), Probe.fixed("b", 100, 0), Probe.fixed("c", 150, 0)), 300);

        Quote best = service.best("X-1").await().atMost(LIMIT);

        assertEquals(new Quote("b", 100), best);
    }

    @Test
    void theSourcesAreAskedAtTheSameTime() {
        QuoteService service = new QuoteService(
                List.of(Probe.fixed("a", 120, 200), Probe.fixed("b", 100, 200), Probe.fixed("c", 150, 200)), 1000);

        long start = System.nanoTime();
        Quote best = service.best("X-1").await().atMost(LIMIT);
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertEquals("b", best.source());
        assertTrue(millis < 450, "three sources of 200 ms took " + millis + " ms: they were asked one after another");
    }

    @Test
    void everySourceIsAskedExactlyOnce() {
        Probe a = Probe.fixed("a", 120, 0);
        Probe b = Probe.fixed("b", 100, 0);
        Probe c = Probe.fixed("c", 150, 0);
        QuoteService service = new QuoteService(List.of(a, b, c), 300);

        service.best("X-1").await().atMost(LIMIT);

        assertEquals(List.of(1, 1, 1), List.of(a.asked(), b.asked(), c.asked()));
    }

    @Test
    void askingTwiceAsksAgain() {
        Probe a = Probe.fixed("a", 120, 0);
        QuoteService service = new QuoteService(List.of(a), 300);

        service.best("X-1").await().atMost(LIMIT);
        service.best("X-1").await().atMost(LIMIT);

        assertEquals(2, a.asked(), "a quote is not cached: each question reaches the source");
    }
}
