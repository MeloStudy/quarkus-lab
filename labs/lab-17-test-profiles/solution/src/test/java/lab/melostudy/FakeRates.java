package lab.melostudy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A tiny stand-in for the remote rates service, built on the JDK's own HTTP server (no extra library).
 * {@code GET /rates?from=USD&to=EUR} answers the rate as text; an unknown pair is 404; a pair that starts
 * with {@code X} simulates a server error (500).
 */
public final class FakeRates {

    private static final Map<String, String> RATES = Map.of(
            "USD-EUR", "0.92",
            "EUR-USD", "1.08",
            "USD-GBP", "0.79",
            "USD-PEN", "3.70");

    private final HttpServer server;

    private FakeRates(HttpServer server) {
        this.server = server;
    }

    /** Starts on a free port. */
    public static FakeRates start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/rates", FakeRates::handle);
            server.start();
            return new FakeRates(server);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String url() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String from = param(query, "from");
        String to = param(query, "to");
        int status;
        String body;
        if (from.startsWith("X")) {
            status = 500;
            body = "boom";
        } else if (RATES.containsKey(from + "-" + to)) {
            status = 200;
            body = RATES.get(from + "-" + to);
        } else {
            status = 404;
            body = "unknown pair";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String param(String query, String name) {
        for (String pair : query == null ? new String[0] : query.split("&")) {
            if (pair.startsWith(name + "=")) {
                return pair.substring(name.length() + 1);
            }
        }
        return "";
    }
}
