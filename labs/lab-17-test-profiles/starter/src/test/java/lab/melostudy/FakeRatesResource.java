package lab.melostudy;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Starts the fake rates server BEFORE the application and tells the application where it is.
 * TODO task 2: Quarkus calls start() once per application start and merges the returned map into the configuration
 * (it wins over application.properties); it calls stop() when the application stops. Today this does nothing.
 */
public class FakeRatesResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        return Map.of();
    }

    @Override
    public void stop() {
    }
}
