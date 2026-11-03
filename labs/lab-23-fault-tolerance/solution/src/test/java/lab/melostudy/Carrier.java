package lab.melostudy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.eclipse.microprofile.config.ConfigProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Remote control of {@link CarrierStub} for the tests. */
public final class Carrier {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    private Carrier() {
    }

    /** Healthy, instant, counters at zero. */
    public static void reset() {
        post("reset");
    }

    /** The next {@code count} quote requests answer {@code status}. Use {@link Integer#MAX_VALUE} for "always". */
    public static void failNext(int count, int status) {
        post("fail?count=" + count + "&status=" + status);
    }

    public static void alwaysFail() {
        failNext(Integer.MAX_VALUE, 503);
    }

    public static void delay(int millis) {
        post("delay?ms=" + millis);
    }

    public static void heal() {
        failNext(0, 503);
        delay(0);
    }

    /** How many quote requests reached the carrier since the last reset. */
    public static int calls() {
        return stats().get("calls").asInt();
    }

    public static int maxConcurrent() {
        return stats().get("maxConcurrent").asInt();
    }

    private static JsonNode stats() {
        try {
            return JSON.readTree(send("GET", "stats").body());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void post(String action) {
        send("POST", action);
    }

    private static HttpResponse<String> send(String method, String action) {
        String base = ConfigProvider.getConfig().getValue("quarkus.rest-client.carrier.url", String.class);
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/__control/" + action))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        try {
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
