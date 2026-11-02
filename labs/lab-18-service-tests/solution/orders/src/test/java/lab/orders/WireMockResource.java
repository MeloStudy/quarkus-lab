package lab.orders;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Starts WireMock, a real HTTP server, BEFORE the application, on a free port, and tells the application where the
 * "inventory service" is. It also hands the server to the test class, so a test can configure what it answers and ask
 * what it received. This works for {@code @QuarkusTest} and for {@code @QuarkusIntegrationTest}: in the second one the
 * application runs in another process and the test class cannot inject beans, but it can still receive this.
 */
public class WireMockResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer server;

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        return Map.of("inventory.url", server.baseUrl());
    }

    @Override
    public void inject(TestInjector injector) {
        injector.injectIntoFields(server, new TestInjector.AnnotatedAndMatchesType(InjectWireMock.class, WireMockServer.class));
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
