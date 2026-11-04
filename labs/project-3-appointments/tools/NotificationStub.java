import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A fake notification provider to run the packaged application by hand:
 * {@code java tools/NotificationStub.java [providerPort] [controlPort] [apiKey]} (defaults: 9090, 9091 and the development key of
 * {@code application.properties}). The tests start their own copy of this class, {@code NotificationStubServer}: keep the two identical
 * (only the name, the {@code main} method and this paragraph differ).
 *
 * <p>A fake notification provider on the JDK's own HTTP server. It behaves like a real one in the way that matters here: <b>it is
 * idempotent</b>. The same {@code Idempotency-Key} is delivered once, however many times it is sent, so a test can count how many
 * messages "really went out" and compare it with what the application believes.
 *
 * <h3>By recipient</h3>
 * <ul>
 *   <li>{@code *@invalid.test}: answers 400 and delivers nothing.</li>
 *   <li>{@code *@broken.test}: answers 500 and delivers nothing.</li>
 *   <li>{@code *@lostanswer.test}: <b>delivers</b>, then closes the connection without answering - the first time that key is seen.</li>
 *   <li>{@code *@slow.test}: <b>delivers</b>, then waits (see {@code /__control/slow}) before answering: the caller times out.</li>
 *   <li>anything else: delivers and answers 200.</li>
 * </ul>
 *
 * <h3>Control (own port, always up)</h3>
 * {@code POST /__control/reset}, {@code /fail?count=N&status=503} (the next N calls fail without being processed),
 * {@code /delay?ms=N} (every call waits N ms first), {@code /slow?ms=N}, {@code /down} and {@code /up} (close and reopen the
 * provider's port), {@code GET /__control/stats}.
 */
public class NotificationStub {

    private static final Pattern TO = Pattern.compile("\"to\"\\s*:\\s*\"([^\"]+)\"");

    private final String expectedAuthorization;
    private final Map<String, String> deliveries = new ConcurrentHashMap<>();
    private final Map<String, Boolean> lostOnce = new ConcurrentHashMap<>();
    private final List<String> seen = new CopyOnWriteArrayList<>();
    private final List<String> traceparents = new CopyOnWriteArrayList<>();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicInteger failuresLeft = new AtomicInteger();
    private volatile int failStatus = 503;
    private volatile int delayMillis;
    private volatile int slowMillis = 3000;

    private HttpServer provider;
    private HttpServer control;
    private int providerPort;

    public NotificationStub(String apiKey) {
        this.expectedAuthorization = "Bearer " + apiKey;
    }

    public void start(int requestedProviderPort, int requestedControlPort) throws IOException {
        provider = newProvider(requestedProviderPort);
        providerPort = provider.getAddress().getPort();
        control = HttpServer.create(new InetSocketAddress("localhost", requestedControlPort), 0);
        control.setExecutor(Executors.newCachedThreadPool());
        control.createContext("/__control/", this::control);
        control.start();
    }

    public static void main(String[] args) throws Exception {
        int providerPort = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
        int controlPort = args.length > 1 ? Integer.parseInt(args[1]) : 9091;
        String apiKey = args.length > 2 ? args[2] : "dev-key-not-a-secret";
        NotificationStub stub = new NotificationStub(apiKey);
        stub.start(providerPort, controlPort);
        System.out.println("Notification provider on http://localhost:" + stub.providerPort() + " - control on http://localhost:" + stub.controlPort()
                + " (stats: GET /__control/stats)");
        Thread.currentThread().join();
    }

    public int providerPort() {
        return providerPort;
    }

    public int controlPort() {
        return control.getAddress().getPort();
    }

    public void stop() {
        if (provider != null) {
            provider.stop(0);
        }
        if (control != null) {
            control.stop(0);
        }
    }

    private HttpServer newProvider(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/messages", this::messages);
        server.start();
        return server;
    }

    // ---------------------------------------------------------------------------------------------------- provider

    private void messages(HttpExchange exchange) throws IOException {
        calls.incrementAndGet();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String key = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        Matcher matcher = TO.matcher(body);
        String to = matcher.find() ? matcher.group(1) : "";
        seen.add(key + " -> " + to);
        String traceparent = exchange.getRequestHeaders().getFirst("traceparent");
        traceparents.add(traceparent == null ? "" : traceparent);

        if (!expectedAuthorization.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            reply(exchange, 401, "{\"error\":\"bad credentials\"}");
            return;
        }
        if (failuresLeft.get() > 0 && failuresLeft.getAndDecrement() > 0) {
            reply(exchange, failStatus, "{\"error\":\"scripted failure\"}");
            return;
        }
        if (delayMillis > 0) {
            sleep(delayMillis);
        }
        if (to.endsWith("@invalid.test")) {
            reply(exchange, 400, "{\"error\":\"unknown address\"}");
            return;
        }
        if (to.endsWith("@broken.test")) {
            reply(exchange, 500, "{\"error\":\"boom\"}");
            return;
        }
        boolean fresh = deliveries.putIfAbsent(key, to) == null;
        if (to.endsWith("@lostanswer.test") && lostOnce.putIfAbsent(key, true) == null) {
            exchange.close(); // delivered, but the answer never arrives
            return;
        }
        if (to.endsWith("@slow.test")) {
            sleep(slowMillis);
        }
        reply(exchange, 200, "{\"status\":\"" + (fresh ? "delivered" : "duplicate") + "\"}");
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---------------------------------------------------------------------------------------------------- control

    private void control(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath().substring("/__control/".length());
        Map<String, String> query = new ConcurrentHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                String[] kv = pair.split("=", 2);
                query.put(kv[0], kv.length > 1 ? kv[1] : "");
            }
        }
        switch (path) {
            case "reset" -> {
                deliveries.clear();
                lostOnce.clear();
                seen.clear();
                traceparents.clear();
                calls.set(0);
                failuresLeft.set(0);
                delayMillis = 0;
                slowMillis = 3000;
                if (provider == null) {
                    provider = newProvider(providerPort);
                }
            }
            case "fail" -> {
                failStatus = Integer.parseInt(query.getOrDefault("status", "503"));
                failuresLeft.set(Integer.parseInt(query.getOrDefault("count", "1")));
            }
            case "delay" -> delayMillis = Integer.parseInt(query.getOrDefault("ms", "0"));
            case "slow" -> slowMillis = Integer.parseInt(query.getOrDefault("ms", "3000"));
            case "down" -> {
                if (provider != null) {
                    provider.stop(0);
                    provider = null;
                }
            }
            case "up" -> {
                if (provider == null) {
                    provider = newProvider(providerPort);
                }
            }
            case "stats" -> {
                String json = "{\"calls\":" + calls.get() + ",\"deliveries\":" + deliveries.size() + ",\"seen\":["
                        + String.join(",", seen.stream().map(s -> "\"" + s + "\"").toList()) + "],\"traceparents\":["
                        + String.join(",", traceparents.stream().map(s -> "\"" + s + "\"").toList()) + "]}";
                reply(exchange, 200, json);
                return;
            }
            default -> {
                reply(exchange, 404, "{}");
                return;
            }
        }
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }
}
