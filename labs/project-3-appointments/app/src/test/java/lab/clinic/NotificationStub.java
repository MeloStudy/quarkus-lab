package lab.clinic;

import java.io.IOException;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/** Starts the fake provider before the application and hands over its URL and the address of its control endpoints. */
public class NotificationStub implements QuarkusTestResourceLifecycleManager {

    /** The same value as {@code %test.notifications.api-key}: the stub refuses any other credentials. */
    static final String API_KEY = "test-key-not-a-secret";

    private NotificationStubServer server;

    @Override
    public Map<String, String> start() {
        try {
            server = new NotificationStubServer(API_KEY);
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the notification stub", e);
        }
        return Map.of(
                "quarkus.rest-client.notifications.url", "http://localhost:" + server.providerPort(),
                "lab.test.notifications-control", "http://localhost:" + server.controlPort());
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
