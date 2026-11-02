package lab.inventory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The contract between the two services: a JSON file that lives OUTSIDE both services (in {@code contracts/}, next to
 * them), because it belongs to neither. Each interaction is one request the consumer makes and the response it relies on.
 */
public final class Contract {

    public record Request(String method, String path, JsonNode body) {
    }

    public record Response(int status, JsonNode body) {
    }

    /** {@code given} is the state the provider must be in for the interaction: sku -> units in stock. */
    public record Interaction(String name, JsonNode given, Request request, Response response) {
    }

    private record File(String consumer, String provider, List<Interaction> interactions) {
    }

    private Contract() {
    }

    public static List<Interaction> load() {
        try {
            // Maven runs the tests with the module folder as working directory
            return new ObjectMapper().readValue(Path.of("..", "contracts", "stock-contract.json").toFile(), File.class)
                    .interactions();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read the contract", e);
        }
    }
}
