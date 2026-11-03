package lab.melostudy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class StockStub implements QuarkusTestResourceLifecycleManager {

    private final List<String> seen = new CopyOnWriteArrayList<>();
    private HttpServer server;

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/reserve", this::reserve);
        server.createContext("/__control/requests", this::requests);
        server.createContext("/__control/reset", exchange -> {
            seen.clear();
            reply(exchange, 204, "");
        });
        server.start();
        String base = "http://localhost:" + server.getAddress().getPort();
        return Map.of("quarkus.rest-client.stock.url", base, "lab.test.stub-url", base);
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void reserve(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String requestId = exchange.getRequestHeaders().getFirst("X-Request-Id");
        String traceparent = exchange.getRequestHeaders().getFirst("traceparent");
        seen.add("{\"requestId\":" + quote(requestId) + ",\"traceparent\":" + quote(traceparent) + "}");
        if (body.contains("SOLD-OUT")) {
            reply(exchange, 409, "{\"error\":\"sold out\"}");
        } else if (body.contains("BROKEN")) {
            reply(exchange, 500, "{\"error\":\"boom\"}");
        } else {
            reply(exchange, 200, "{\"reserved\":true}");
        }
    }

    private void requests(HttpExchange exchange) throws IOException {
        reply(exchange, 200, "[" + String.join(",", seen) + "]");
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }
}
