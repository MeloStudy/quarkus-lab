package lab.melostudy;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.arc.All;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Asks every source and answers with the cheapest quote. Tasks 2 and 3: the sources are asked <b>at the same time</b>, and a
 * source that fails, is slow or never answers must not spoil the answer of the others.
 */
@ApplicationScoped
public class QuoteService {

    private static final Logger LOG = Logger.getLogger(QuoteService.class);

    private final List<QuoteSource> sources;
    private final Duration timeout;

    @Inject
    public QuoteService(@All List<QuoteSource> sources, @ConfigProperty(name = "app.quotes.timeout-millis", defaultValue = "300") long timeoutMillis) {
        this.sources = sources;
        this.timeout = Duration.ofMillis(timeoutMillis);
    }

    public Uni<Quote> best(String sku) {
        List<Uni<Quote>> asks = sources.stream().map(source -> ask(source, sku)).toList();
        return Uni.join().all(asks).andFailFast()
                .map(answers -> answers.stream()
                        .filter(Objects::nonNull)
                        .min(Comparator.comparingLong(Quote::cents))
                        .orElseThrow(() -> new NoQuoteException(sku)));
    }

    /**
     * One source: a failure is retried twice (20 ms, then 40 ms apart); the whole question, retries included, has {@code timeout}
     * to answer. Whatever goes wrong becomes "no quote from this source" ({@code null}), never a failure of the whole, and it is logged
     * first: a recovery that says nothing hides bugs.
     */
    private Uni<Quote> ask(QuoteSource source, String sku) {
        return source.quote(sku)
                .onFailure().retry().withBackOff(Duration.ofMillis(20)).withJitter(0).atMost(2)
                .ifNoItem().after(timeout).fail()
                .onFailure().invoke(failure -> LOG.warnf("Source %s gave no quote for %s: %s", source.name(), sku, failure.toString()))
                .onFailure().recoverWithNull();
    }
}
