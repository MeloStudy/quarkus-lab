import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A fake stock service for running the packaged jar by hand (the tests start their own, on a random port).
 *
 * <pre>java tools/StockStub.java 9998</pre>
 *
 * {@code POST /reserve}: 409 when the body mentions SOLD-OUT, 500 when it mentions BROKEN, 200 otherwise.
 * It prints the two headers this lab cares about, so you can see what travelled with each call.
 */
public class StockStub {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9998;
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/reserve", StockStub::reserve);
        server.start();
        System.out.println("stock stub listening on http://localhost:" + port + "/reserve (Ctrl+C to stop)");
    }

    private static void reserve(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("stock: " + exchange.getRequestMethod() + " " + exchange.getRequestURI()
                + "  X-Request-Id=" + exchange.getRequestHeaders().getFirst("X-Request-Id")
                + "  traceparent=" + exchange.getRequestHeaders().getFirst("traceparent"));
        int status = body.contains("SOLD-OUT") ? 409 : body.contains("BROKEN") ? 500 : 200;
        byte[] answer = ("{\"reserved\":" + (status == 200) + "}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, answer.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(answer);
        }
    }
}
