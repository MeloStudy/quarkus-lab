package lab.orders;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Starts WireMock, a real HTTP server, BEFORE the application, on a free port, and tells the application where the
 * "inventory service" is. It also hands the server to the test class, so a test can configure what it answers and ask
 * what it received. This works for {@code @QuarkusTest} and for {@code @QuarkusIntegrationTest}: in the second one the
 * application runs in another process and the test class cannot inject beans, but it can still receive this.
 */
public class WireMockResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer server;

    /** TODO task 1: start a WireMockServer on a free port and give the application its URL as 'inventory.url'. */
    @Override
    public Map<String, String> start() {
        return Map.of();
    }

    /** TODO task 1: hand the server to the test class: the fields annotated with @InjectWireMock. */
    @Override
    public void inject(TestInjector injector) {
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
