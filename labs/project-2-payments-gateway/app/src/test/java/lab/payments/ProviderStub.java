package lab.payments;

import java.io.IOException;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/** Starts the fake provider before the application and hands over its URL and the address of its control endpoints. */
public class ProviderStub implements QuarkusTestResourceLifecycleManager {

    /** The same value as {@code %test.payments.provider.api-key}: the stub refuses any other credentials. */
    static final String API_KEY = "test-key-not-a-secret";

    private ProviderStubServer server;

    @Override
    public Map<String, String> start() {
        try {
            server = new ProviderStubServer(API_KEY);
            server.start(0, 0);
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the provider stub", e);
        }
        return Map.of(
                "quarkus.rest-client.provider.url", "http://localhost:" + server.providerPort(),
                "lab.test.provider-control", "http://localhost:" + server.controlPort());
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
