package lab.melostudy;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

/**
 * Three fake suppliers for the running application. Their prices are fixed by SKU ({@code alpha} is always the cheapest), and
 * their behavior can be changed from the tests: each can be slowed down or made to fail.
 */
@ApplicationScoped
public class DemoSources {

    /** What a demo source does right now. */
    public static class Behavior {
        public volatile long delayMillis = 20;
        public volatile boolean failing;
    }

    private final Map<String, Behavior> behaviors = new ConcurrentHashMap<>();

    public Behavior behavior(String name) {
        return behaviors.computeIfAbsent(name, key -> new Behavior());
    }

    public void reset() {
        behaviors.clear();
    }

    @Produces
    @Dependent
    QuoteSource alpha() {
        return source("alpha", 0);
    }

    @Produces
    @Dependent
    QuoteSource beta() {
        return source("beta", 5);
    }

    @Produces
    @Dependent
    QuoteSource gamma() {
        return source("gamma", 10);
    }

    private QuoteSource source(String name, int markup) {
        return new QuoteSource() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Uni<Quote> quote(String sku) {
                Behavior behavior = behavior(name);
                if (behavior.failing) {
                    return Uni.createFrom().failure(new IllegalStateException(name + " is down"));
                }
                long cents = 1000 + Math.abs(sku.hashCode() % 100) + markup;
                return Uni.createFrom().item(new Quote(name, cents)).onItem().delayIt().by(Duration.ofMillis(behavior.delayMillis));
            }
        };
    }
}
