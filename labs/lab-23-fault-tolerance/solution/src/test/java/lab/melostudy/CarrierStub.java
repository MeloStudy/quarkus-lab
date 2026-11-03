package lab.melostudy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * A fake "carrier" on a random free port, built on the JDK's own HTTP server (no dependency, no internet).
 * Its behavior is scripted over HTTP, so the tests control it from any classloader:
 * <ul>
 *   <li>{@code POST /__control/reset}: healthy, instant, counters at zero</li>
 *   <li>{@code POST /__control/fail?count=N&status=503}: the next N quote requests answer that status</li>
 *   <li>{@code POST /__control/delay?ms=N}: every quote request waits N ms before answering</li>
 *   <li>{@code GET /__control/stats}: {@code {"calls":n,"maxConcurrent":m}}</li>
 * </ul>
 * Quotes: {@code Lima} 12.5 and {@code Quito} 9.8. Any other city is a 404.
 */
public class CarrierStub implements QuarkusTestResourceLifecycleManager {

    private static final Map<String, String> PRICES = Map.of("Lima", "12.5", "Quito", "9.8");

    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicInteger current = new AtomicInteger();
    private final AtomicInteger maxConcurrent = new AtomicInteger();
    private final AtomicInteger failuresLeft = new AtomicInteger();
    private volatile int failStatus = 503;
    private volatile int delayMillis;
    private HttpServer server;

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the carrier stub", e);
        }
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/quote/", this::quote);
        server.createContext("/__control/", this::control);
        server.start();
        return Map.of("quarkus.rest-client.carrier.url", "http://localhost:" + server.getAddress().getPort());
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void quote(HttpExchange exchange) throws IOException {
        calls.incrementAndGet();
        int now = current.incrementAndGet();
        maxConcurrent.accumulateAndGet(now, Math::max);
        try {
            if (delayMillis > 0) {
                Thread.sleep(delayMillis);
            }
            if (failuresLeft.get() > 0 && failuresLeft.getAndDecrement() > 0) {
                reply(exchange, failStatus, "{\"error\":\"carrier is having a bad day\"}");
                return;
            }
            String city = exchange.getRequestURI().getPath().substring("/quote/".length());
            String price = PRICES.get(city);
            if (price == null) {
                reply(exchange, 404, "{\"error\":\"unknown city\"}");
            } else {
                reply(exchange, 200, "{\"city\":\"" + city + "\",\"price\":" + price + "}");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // the client gave up (timeout): nothing to do
        } finally {
            current.decrementAndGet();
            exchange.close();
        }
    }

    private void control(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String action = uri.getPath().substring("/__control/".length());
        Map<String, String> query = parse(uri.getRawQuery());
        switch (action) {
            case "reset" -> {
                calls.set(0);
                current.set(0);
                maxConcurrent.set(0);
                failuresLeft.set(0);
                failStatus = 503;
                delayMillis = 0;
            }
            case "fail" -> {
                failuresLeft.set(Integer.parseInt(query.getOrDefault("count", "1")));
                failStatus = Integer.parseInt(query.getOrDefault("status", "503"));
            }
            case "delay" -> delayMillis = Integer.parseInt(query.getOrDefault("ms", "0"));
            case "stats" -> {
                reply(exchange, 200, "{\"calls\":" + calls.get() + ",\"maxConcurrent\":" + maxConcurrent.get() + "}");
                return;
            }
            default -> {
                reply(exchange, 404, "{}");
                return;
            }
        }
        reply(exchange, 200, "{}");
    }

    private static Map<String, String> parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return Map.of();
        }
        java.util.HashMap<String, String> result = new java.util.HashMap<>();
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(parts[0], parts.length > 1 ? parts[1] : "");
        }
        return result;
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
