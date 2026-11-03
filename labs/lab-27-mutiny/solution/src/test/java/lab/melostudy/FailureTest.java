package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Task 3: sources fail, are slow, or never answer. The answer of the others must survive, and must not wait longer than the timeout. */
class FailureTest {

    private static final Duration LIMIT = Duration.ofSeconds(3);

    @Test
    void aFailingSourceDoesNotSpoilTheOthers() {
        QuoteService service = new QuoteService(List.of(Probe.failing("down"), Probe.fixed("up", 150, 0)), 300);

        assertEquals("up", service.best("X-1").await().atMost(LIMIT).source());
    }

    @Test
    void aFailingSourceIsAskedOnceAndRetriedTwice() {
        Probe down = Probe.failing("down");
        QuoteService service = new QuoteService(List.of(down, Probe.fixed("up", 150, 0)), 300);

        service.best("X-1").await().atMost(LIMIT);

        assertEquals(3, down.asked(), "one try and two retries");
    }

    @Test
    void aFlakySourceThatRecoversInTimeIsUsed() {
        Probe flaky = Probe.flaky("flaky", 2, 130);
        QuoteService service = new QuoteService(List.of(flaky, Probe.fixed("steady", 150, 0)), 300);

        Quote best = service.best("X-1").await().atMost(LIMIT);

        assertEquals(new Quote("flaky", 130), best);
        assertEquals(3, flaky.asked());
    }

    @Test
    void aSourceThatNeverAnswersIsIgnoredAfterTheTimeout() {
        QuoteService service = new QuoteService(List.of(Probe.never("silent"), Probe.fixed("up", 150, 10)), 300);

        long start = System.nanoTime();
        Quote best = service.best("X-1").await().atMost(LIMIT);
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertEquals("up", best.source());
        assertTrue(millis < 550, "the answer took " + millis + " ms: the timeout is 300 ms");
    }

    @Test
    void theTimeoutLimitsTheWholeQuestionAndNotEachAttempt() {
        Probe silent = Probe.never("silent");
        QuoteService service = new QuoteService(List.of(silent, Probe.fixed("up", 150, 0)), 300);

        service.best("X-1").await().atMost(LIMIT);

        assertEquals(1, silent.asked(), "a source that does not answer in time is not asked again: there is no time left");
    }

    @Test
    void aSlowButNotSilentSourceStillCounts() {
        QuoteService service = new QuoteService(List.of(Probe.fixed("slow", 90, 150), Probe.fixed("fast", 150, 0)), 300);

        assertEquals("slow", service.best("X-1").await().atMost(LIMIT).source());
    }

    @Test
    void whenNoSourceAnswersThereIsNoQuote() {
        QuoteService service = new QuoteService(List.of(Probe.failing("down"), Probe.never("silent")), 150);

        NoQuoteException failure = assertThrows(NoQuoteException.class, () -> service.best("X-9").await().atMost(LIMIT));

        assertTrue(failure.getMessage().contains("X-9"), "the message names the SKU");
    }
}
