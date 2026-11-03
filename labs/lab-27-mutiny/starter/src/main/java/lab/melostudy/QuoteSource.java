package lab.melostudy;

import io.smallrye.mutiny.Uni;

/**
 * One supplier that quotes a price. Asking is asynchronous and may take long, fail or never answer: each subscription to the
 * {@code Uni} is one question.
 */
public interface QuoteSource {

    String name();

    Uni<Quote> quote(String sku);
}
