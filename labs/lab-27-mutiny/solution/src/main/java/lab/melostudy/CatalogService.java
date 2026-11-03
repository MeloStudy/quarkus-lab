package lab.melostudy;

import java.util.List;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Task 4: the best quote of many SKUs, without asking for all of them at once. */
@ApplicationScoped
public class CatalogService {

    /** At most this many SKUs are being quoted at any moment. */
    static final int MAX_IN_FLIGHT = 4;

    private final QuoteService quotes;

    @Inject
    public CatalogService(QuoteService quotes) {
        this.quotes = quotes;
    }

    public Multi<Quote> bestOfAll(List<String> skus) {
        return Multi.createFrom().iterable(skus)
                .onItem().transformToUni(quotes::best).merge(MAX_IN_FLIGHT);
    }
}
