package lab.melostudy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import io.smallrye.mutiny.Uni;

/**
 * A quote source for the tests that counts how many times it was asked. Each <b>subscription</b> to the {@code Uni} it returns
 * is one question, so a retry shows up as one more.
 */
final class Probe implements QuoteSource {

    private final String name;
    private final BiFunction<Integer, String, Uni<Quote>> behavior;
    private final AtomicInteger asked = new AtomicInteger();

    private Probe(String name, BiFunction<Integer, String, Uni<Quote>> behavior) {
        this.name = name;
        this.behavior = behavior;
    }

    /** Answers {@code cents} after {@code delayMillis}. */
    static Probe fixed(String name, long cents, long delayMillis) {
        return new Probe(name, (attempt, sku) -> {
            Uni<Quote> answer = Uni.createFrom().item(new Quote(name, cents));
            // delayIt() refuses a duration of zero
            return delayMillis > 0 ? answer.onItem().delayIt().by(Duration.ofMillis(delayMillis)) : answer;
        });
    }

    /** Fails every time, at once. */
    static Probe failing(String name) {
        return new Probe(name, (attempt, sku) -> Uni.createFrom().failure(new IllegalStateException(name + " is down")));
    }

    /** Never answers and never fails. */
    static Probe never(String name) {
        return new Probe(name, (attempt, sku) -> Uni.createFrom().emitter(emitter -> {
        }));
    }

    /** Fails the first {@code failures} times it is asked, then answers {@code cents}. */
    static Probe flaky(String name, int failures, long cents) {
        return new Probe(name, (attempt, sku) -> attempt <= failures
                ? Uni.createFrom().failure(new IllegalStateException(name + " failed on attempt " + attempt))
                : Uni.createFrom().item(new Quote(name, cents)));
    }

    /** Answers after {@code delayMillis} and keeps track of how many questions are waiting at the same time. */
    static Probe tracked(String name, long delayMillis, AtomicInteger inFlight, AtomicInteger maxInFlight) {
        return new Probe(name, (attempt, sku) -> Uni.createFrom().item(new Quote(name, 100 + Integer.parseInt(sku.substring(sku.indexOf('-') + 1))))
                .onItem().invoke(() -> maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max))
                .onItem().delayIt().by(Duration.ofMillis(delayMillis))
                .onItem().invoke(inFlight::decrementAndGet));
    }

    int asked() {
        return asked.get();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Uni<Quote> quote(String sku) {
        return Uni.createFrom().deferred(() -> behavior.apply(asked.incrementAndGet(), sku));
    }
}
