package lab.orders;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Talks to the inventory service. Every way it can go wrong is translated into one of FOUR exceptions, so the rest of
 * the application never sees HTTP status codes, timeouts or JSON parsing errors.
 */
@ApplicationScoped
public class InventoryClient {

    public static class UnknownSkuException extends RuntimeException {
        public UnknownSkuException(String sku) {
            super("unknown product " + sku);
        }
    }

    public static class OutOfStockException extends RuntimeException {
        public OutOfStockException(String sku) {
            super("not enough stock of " + sku);
        }
    }

    /** The inventory service answered, but not with something we can use (5xx, garbage, connection broken). */
    public static class InventoryFailedException extends RuntimeException {
        public InventoryFailedException(String message) {
            super(message);
        }
    }

    /** The inventory service did not answer in time. */
    public static class InventoryTimeoutException extends RuntimeException {
        public InventoryTimeoutException() {
            super("the inventory service did not answer in time");
        }
    }

    private final String baseUrl;
    private final Duration timeout;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();

    InventoryClient(@ConfigProperty(name = "inventory.url") String baseUrl,
            @ConfigProperty(name = "inventory.timeout") Duration timeout, ObjectMapper json) {
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.json = json;
    }

    public int available(String sku) {
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(baseUrl + "/stock/" + sku)).GET());
        return switch (response.statusCode()) {
            case 200 -> field(response, "available");
            case 404 -> throw new UnknownSkuException(sku);
            default -> throw new InventoryFailedException("inventory answered " + response.statusCode());
        };
    }

    public void reserve(String sku, int quantity) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + "/stock/" + sku + "/reserve"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"quantity\":" + quantity + "}"));
        HttpResponse<String> response = send(request);
        switch (response.statusCode()) {
            case 200 -> field(response, "reserved");
            case 404 -> throw new UnknownSkuException(sku);
            case 409 -> throw new OutOfStockException(sku);
            default -> throw new InventoryFailedException("inventory answered " + response.statusCode());
        }
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        try {
            return http.send(request.timeout(timeout).build(), HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new InventoryTimeoutException();
        } catch (IOException e) {
            throw new InventoryFailedException("inventory not reachable: " + e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InventoryFailedException("interrupted");
        }
    }

    /** Reads an integer field of a successful answer; a body that is not what the contract promises is a failure. */
    private int field(HttpResponse<String> response, String name) {
        try {
            JsonNode node = json.readTree(response.body());
            if (node == null || !node.path(name).isInt()) {
                throw new InventoryFailedException("inventory answer without an integer '" + name + "'");
            }
            return node.get(name).asInt();
        } catch (JsonProcessingException e) {
            throw new InventoryFailedException("inventory answer is not JSON");
        }
    }
}
