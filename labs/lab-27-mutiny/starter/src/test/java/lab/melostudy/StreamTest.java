package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;

/** Task 4: streams. Concurrency has to be limited on purpose, and a subscriber that cannot keep up must not break the stream. */
class StreamTest {

    private static final Duration LIMIT = Duration.ofSeconds(10);

    @Test
    void everySkuGetsItsQuote() {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        CatalogService catalog = new CatalogService(new QuoteService(List.of(Probe.tracked("only", 30, inFlight, max)), 300));
        List<String> skus = IntStream.range(0, 12).mapToObj(i -> "S-" + i).toList();

        List<Quote> quotes = catalog.bestOfAll(skus).collect().asList().await().atMost(LIMIT);

        Set<Long> cents = quotes.stream().map(Quote::cents).collect(Collectors.toSet());
        assertEquals(IntStream.range(0, 12).mapToObj(i -> 100L + i).collect(Collectors.toSet()), cents);
    }

    @Test
    void atMostFourSkusAreQuotedAtTheSameTime() {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        CatalogService catalog = new CatalogService(new QuoteService(List.of(Probe.tracked("only", 60, inFlight, max)), 300));
        List<String> skus = IntStream.range(0, 20).mapToObj(i -> "S-" + i).toList();

        List<Quote> quotes = catalog.bestOfAll(skus).collect().asList().await().atMost(LIMIT);

        assertEquals(20, quotes.size());
        assertTrue(max.get() <= 4, "up to " + max.get() + " SKUs were being quoted at once");
        assertTrue(max.get() >= 2, "the SKUs were quoted one after another: nothing ran concurrently");
    }

    @Test
    void aSubscriberThatAsksForLittleDoesNotMakeTheReadingsFail() throws InterruptedException {
        AssertSubscriber<Long> subscriber = new SensorService().readings().subscribe().withSubscriber(AssertSubscriber.create(3));

        Thread.sleep(150); // the sensor reported about a hundred times; the subscriber asked for three

        assertNull(subscriber.getFailure(), "the stream failed: " + subscriber.getFailure());
        assertEquals(List.of(0L, 1L, 2L), subscriber.getItems());

        subscriber.request(2); // asking again gets the next readings, the ones that are current
        Thread.sleep(50);
        assertNull(subscriber.getFailure());
        assertEquals(5, subscriber.getItems().size());
        subscriber.cancel();
    }
}
