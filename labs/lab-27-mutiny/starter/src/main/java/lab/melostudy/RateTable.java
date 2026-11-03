package lab.melostudy;

import java.util.Map;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * TODO Task 1: the exchange rates, loaded when somebody first needs them and then shared by everybody. The loader is blocking, so
 * the loading must not run on the thread that subscribes (lab 26).
 */
@ApplicationScoped
public class RateTable {

    private final Uni<Map<String, Double>> rates;

    @Inject
    public RateTable(RateLoader loader) {
        this.rates = Uni.createFrom().item(loader.load());
    }

    public Uni<Map<String, Double>> rates() {
        return rates;
    }
}
