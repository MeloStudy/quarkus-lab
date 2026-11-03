package lab.payments;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
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
 * A fake card provider built on the JDK's own HTTP server (no dependency, no internet). It behaves like a real one in
 * the one way that matters here: <b>it is idempotent</b>. The same {@code Idempotency-Key} charges once and gets the same
 * answer every time. What it does with a request depends on the card token (behavior per request) or on the control
 * endpoints (behavior over time).
 *
 * <h3>Tokens</h3>
 * <ul>
 *   <li>{@code tok_declined*}: the charge is recorded as declined; answers 402.</li>
 *   <li>{@code tok_rejected*}: answers 400, nothing is recorded.</li>
 *   <li>{@code tok_broken*}: answers 500, nothing is recorded.</li>
 *   <li>{@code tok_slowcapture*}, or any token ending in {@code slow} ({@code tok_declinedslow}): records the result, then waits (see
 *       {@code /__control/slow}) before answering: the caller times out although the provider already decided.</li>
 *   <li>{@code tok_slowfirst*}: like the above, but only the first time the key is seen; a retry gets the answer at once.</li>
 *   <li>{@code tok_dropconn*}: <b>charges</b>, then closes the connection without an answer.</li>
 *   <li>{@code tok_garbage*}: <b>charges</b>, then answers 200 with a body that is not JSON.</li>
 *   <li>anything else: charges and answers 200.</li>
 * </ul>
 *
 * <h3>Control (own port, always up)</h3>
 * {@code POST /__control/reset}, {@code /fail?count=N&status=503&on=charge|refund|lookup|all} (the next N calls fail without
 * being processed), {@code /delay?ms=N} (every provider call waits N ms first), {@code /slow?ms=N}, {@code /down} and
 * {@code /up} (close and reopen the provider's port), {@code /crash-after?calls=N} (the port closes while the Nth call is being
 * handled), {@code /lose-refund-answers?count=N} (the next N refunds are processed and the connection is closed without an answer),
 * {@code GET /__control/stats}.
 */
public class ProviderStubServer {

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    private record Result(String id, String status, String reason) {
    }

    private final String expectedAuthorization;
    private final Map<String, Result> charges = new ConcurrentHashMap<>();
    private final Map<String, Result> refunds = new ConcurrentHashMap<>();
    private final List<String> seen = new CopyOnWriteArrayList<>();
    private final AtomicInteger ids = new AtomicInteger();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicInteger realCharges = new AtomicInteger();
    private final AtomicInteger realRefunds = new AtomicInteger();
    private final AtomicInteger failuresLeft = new AtomicInteger();
    private volatile int failStatus = 503;
    private volatile String failOn = "charge";
    private volatile int delayMillis;
    private volatile int slowMillis = 3000;
    private volatile int crashAfterCalls;
    private final AtomicInteger lossesLeft = new AtomicInteger();

    private HttpServer provider;
    private HttpServer control;
    private int providerPort;

    public ProviderStubServer(String apiKey) {
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
        server.createContext("/charges", this::charges);
        server.start();
        return server;
    }

    // ---------------------------------------------------------------------------------------------------- provider

    private void charges(HttpExchange exchange) throws IOException {
        int callNumber = calls.incrementAndGet();
        if (crashAfterCalls > 0 && callNumber == crashAfterCalls) {
            new Thread(this::crash).start(); // the provider "dies" while handling this call: later calls find nobody listening
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String key = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        String query = exchange.getRequestURI().getRawQuery();
        record(method, path, key, query, exchange);

        if (!expectedAuthorization.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            reply(exchange, 401, "{\"error\":\"bad credentials\"}");
            return;
        }
        String operation = path.endsWith("/refund") ? "refund" : "GET".equals(method) ? "lookup" : "charge";
        if (shouldFail(operation)) {
            reply(exchange, failStatus, "{\"error\":\"scripted failure\"}");
            return;
        }
        if (delayMillis > 0) {
            sleep(delayMillis);
        }
        switch (operation) {
            case "lookup" -> lookup(exchange, query);
            case "refund" -> refund(exchange, path, key);
            default -> charge(exchange, body, key);
        }
    }

    private void charge(HttpExchange exchange, String body, String key) throws IOException {
        Matcher matcher = TOKEN.matcher(body);
        String token = matcher.find() ? matcher.group(1) : "";
        if (token.startsWith("tok_rejected")) {
            reply(exchange, 400, "{\"error\":\"bad request\"}");
            return;
        }
        if (token.startsWith("tok_broken")) {
            reply(exchange, 500, "{\"error\":\"boom\"}");
            return;
        }
        boolean[] fresh = { false };
        Result result = charges.computeIfAbsent(key, k -> {
            fresh[0] = true;
            boolean declined = token.startsWith("tok_declined");
            if (!declined) {
                realCharges.incrementAndGet(); // "charges" counts the times money moved
            }
            return new Result("ch_" + ids.incrementAndGet(), declined ? "declined" : "captured", declined ? "card_declined" : null);
        });
        if (token.startsWith("tok_slowcapture") || token.endsWith("slow") || (token.startsWith("tok_slowfirst") && fresh[0])) {
            sleep(slowMillis);
        } else if (token.startsWith("tok_dropconn")) {
            exchange.close();
            return;
        } else if (token.startsWith("tok_garbage")) {
            reply(exchange, 200, "<html>this is not json</html>");
            return;
        }
        replyWith(exchange, result);
    }

    private void lookup(HttpExchange exchange, String query) throws IOException {
        String key = query == null ? "" : query.replaceFirst("^key=", "");
        Result result = charges.get(key);
        if (result == null) {
            reply(exchange, 404, "{\"error\":\"unknown key\"}");
        } else {
            reply(exchange, 200, json(result));
        }
    }

    private void refund(HttpExchange exchange, String path, String key) throws IOException {
        String chargeId = path.substring("/charges/".length(), path.length() - "/refund".length());
        boolean known = charges.values().stream().anyMatch(r -> r.id().equals(chargeId) && "captured".equals(r.status()));
        if (!known) {
            reply(exchange, 404, "{\"error\":\"unknown charge\"}");
            return;
        }
        Result result = refunds.computeIfAbsent(key, k -> {
            realRefunds.incrementAndGet();
            return new Result(chargeId, "refunded", null);
        });
        if (lossesLeft.get() > 0 && lossesLeft.getAndDecrement() > 0) {
            exchange.close(); // processed, but the answer never arrives
            return;
        }
        reply(exchange, 200, json(result));
    }

    private void replyWith(HttpExchange exchange, Result result) throws IOException {
        if ("declined".equals(result.status())) {
            reply(exchange, 402, json(result));
        } else {
            reply(exchange, 200, json(result));
        }
    }

    private static String json(Result r) {
        return "{\"id\":\"" + r.id() + "\",\"status\":\"" + r.status() + "\""
                + (r.reason() == null ? "" : ",\"reason\":\"" + r.reason() + "\"") + "}";
    }

    private boolean shouldFail(String operation) {
        if (failuresLeft.get() <= 0 || !("all".equals(failOn) || failOn.equals(operation))) {
            return false;
        }
        return failuresLeft.getAndDecrement() > 0;
    }

    private void record(String method, String path, String key, String query, HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        seen.add("{\"call\":" + quote(method + " " + path + (query == null ? "" : "?" + query)) + ",\"key\":" + quote(key)
                + ",\"authorization\":" + quote(authorization)
                + ",\"requestId\":" + quote(exchange.getRequestHeaders().getFirst("X-Request-Id"))
                + ",\"traceparent\":" + quote(exchange.getRequestHeaders().getFirst("traceparent")) + "}");
    }

    // ----------------------------------------------------------------------------------------------------- control

    private void control(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String action = uri.getPath().substring("/__control/".length());
        Map<String, String> params = new ConcurrentHashMap<>();
        if (uri.getRawQuery() != null) {
            for (String pair : uri.getRawQuery().split("&")) {
                String[] kv = pair.split("=", 2);
                params.put(kv[0], kv.length > 1 ? kv[1] : "");
            }
        }
        switch (action) {
            case "reset" -> {
                charges.clear();
                refunds.clear();
                seen.clear();
                ids.set(0);
                calls.set(0);
                realCharges.set(0);
                realRefunds.set(0);
                failuresLeft.set(0);
                failStatus = 503;
                failOn = "charge";
                delayMillis = 0;
                slowMillis = 3000;
                crashAfterCalls = 0;
                lossesLeft.set(0);
                ensureUp();
                reply(exchange, 204, "");
            }
            case "fail" -> {
                failStatus = Integer.parseInt(params.getOrDefault("status", "503"));
                failOn = params.getOrDefault("on", "charge");
                failuresLeft.set(Integer.parseInt(params.getOrDefault("count", "1")));
                reply(exchange, 204, "");
            }
            case "delay" -> {
                delayMillis = Integer.parseInt(params.getOrDefault("ms", "0"));
                reply(exchange, 204, "");
            }
            case "slow" -> {
                slowMillis = Integer.parseInt(params.getOrDefault("ms", "3000"));
                reply(exchange, 204, "");
            }
            case "lose-refund-answers" -> {
                lossesLeft.set(Integer.parseInt(params.getOrDefault("count", "1")));
                reply(exchange, 204, "");
            }
            case "crash-after" -> {
                crashAfterCalls = Integer.parseInt(params.getOrDefault("calls", "1"));
                reply(exchange, 204, "");
            }
            case "down" -> {
                if (provider != null) {
                    provider.stop(0);
                    provider = null;
                }
                reply(exchange, 204, "");
            }
            case "up" -> {
                ensureUp();
                reply(exchange, 204, "");
            }
            case "stats" -> reply(exchange, 200, "{\"calls\":" + calls.get() + ",\"charges\":" + realCharges.get()
                    + ",\"refunds\":" + realRefunds.get() + ",\"seen\":[" + String.join(",", seen) + "]}");
            default -> reply(exchange, 404, "{}");
        }
    }

    private synchronized void crash() {
        if (provider != null) {
            provider.stop(0);
            provider = null;
        }
    }

    private synchronized void ensureUp() throws IOException {
        if (provider == null) {
            provider = newProvider(providerPort);
        }
    }

    // ------------------------------------------------------------------------------------------------------ helpers

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
        } catch (IOException clientGone) {
            // the caller gave up (timeout); nothing to do
        } finally {
            exchange.close();
        }
    }
}
