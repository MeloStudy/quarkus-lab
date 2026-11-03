package lab.melostudy;

import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Task 1: the exchange rates, loaded when somebody first needs them and then shared by everybody. The loader is blocking, so the
 * loading is moved to a worker thread (lab 26) and never runs on the thread that subscribes.
 */
@ApplicationScoped
public class RateTable {

    private final Uni<Map<String, Double>> rates;

    @Inject
    public RateTable(RateLoader loader) {
        this.rates = Uni.createFrom().item(loader::load)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .memoize().indefinitely();
    }

    public Uni<Map<String, Double>> rates() {
        return rates;
    }
}
