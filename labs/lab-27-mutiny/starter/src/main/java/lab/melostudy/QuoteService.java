package lab.melostudy;

import java.time.Duration;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.All;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Asks every source and answers with the cheapest quote.
 * <p>
 * It works when everything works, and nothing more: the sources are asked one after another and the first one that fails spoils
 * the answer. TODO Task 2: ask them at the same time. TODO Task 3: a source that fails, is slow or never answers must not spoil
 * the answer of the others (see the README for the rules).
 */
@ApplicationScoped
public class QuoteService {

    private final List<QuoteSource> sources;
    private final Duration timeout;

    @Inject
    public QuoteService(@All List<QuoteSource> sources, @ConfigProperty(name = "app.quotes.timeout-millis", defaultValue = "300") long timeoutMillis) {
        this.sources = sources;
        this.timeout = Duration.ofMillis(timeoutMillis);
    }

    public Uni<Quote> best(String sku) {
        Uni<Quote> best = Uni.createFrom().nullItem();
        for (QuoteSource source : sources) {
            best = best.chain(current -> source.quote(sku).map(candidate -> cheaper(current, candidate)));
        }
        return best.onItem().ifNull().failWith(() -> new NoQuoteException(sku));
    }

    private static Quote cheaper(Quote current, Quote candidate) {
        return current == null || candidate.cents() < current.cents() ? candidate : current;
    }
}
