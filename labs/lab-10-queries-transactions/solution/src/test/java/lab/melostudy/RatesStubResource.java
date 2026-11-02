package lab.melostudy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * A fake "remote exchange-rate service" on a random free port, built on the JDK's own HTTP server
 * (no extra dependency, no internet). It behaves according to the currency asked for:
 * <ul>
 *   <li>{@code EUR}, {@code GBP}: normal answers</li>
 *   <li>{@code XXX}: 404 (unknown currency)</li>
 *   <li>{@code ERR}: 500</li>
 *   <li>{@code SLO}: answers after 3 seconds (slower than the test read timeout)</li>
 *   <li>{@code BAD}: 200 with a body that is not JSON</li>
 *   <li>{@code ZER}: 200 with a rate of 0 (valid JSON, unusable value)</li>
 * </ul>
 * Its URL is injected through {@code quarkus.rest-client.rates.url}, which beats application.properties.
 */
public class RatesStubResource implements QuarkusTestResourceLifecycleManager {

    private static final Map<String, String> RATES = Map.of("EUR", "0.92", "GBP", "0.79");

    private HttpServer server;

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the rates stub", e);
        }
        server.createContext("/rates/", this::handle);
        server.start();
        return Map.of("quarkus.rest-client.rates.url", "http://localhost:" + server.getAddress().getPort());
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        String currency = exchange.getRequestURI().getPath().substring("/rates/".length());
        try {
            switch (currency) {
                case "XXX" -> reply(exchange, 404, "{\"error\":\"unknown currency\"}");
                case "ERR" -> reply(exchange, 500, "{\"error\":\"boom\"}");
                case "BAD" -> reply(exchange, 200, "this is not json");
                case "ZER" -> reply(exchange, 200, "{\"currency\":\"ZER\",\"rate\":0}");
                case "SLO" -> {
                    Thread.sleep(3000);
                    reply(exchange, 200, "{\"currency\":\"SLO\",\"rate\":1.0}");
                }
                default -> {
                    String rate = RATES.get(currency);
                    if (rate == null) {
                        reply(exchange, 404, "{\"error\":\"unknown currency\"}");
                    } else {
                        reply(exchange, 200, "{\"currency\":\"" + currency + "\",\"rate\":" + rate + "}");
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // the client gave up (timeout): nothing to do
        } finally {
            exchange.close();
        }
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
