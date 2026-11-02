package lab.melostudy;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Starts the fake rates server BEFORE the application and tells the application where it is. Quarkus calls
 * {@link #start()} once per application start, merges the returned map into the configuration (it wins over
 * {@code application.properties}), and calls {@link #stop()} when the application stops.
 */
public class FakeRatesResource implements QuarkusTestResourceLifecycleManager {

    private FakeRates rates;

    @Override
    public Map<String, String> start() {
        rates = FakeRates.start();
        return Map.of("rates.url", rates.url());
    }

    @Override
    public void stop() {
        if (rates != null) {
            rates.stop();
        }
    }
}
