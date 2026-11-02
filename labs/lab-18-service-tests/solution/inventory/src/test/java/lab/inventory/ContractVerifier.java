package lab.inventory;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/** The comparison rule of the provider verification. */
public final class ContractVerifier {

    private ContractVerifier() {
    }

    /**
     * Is {@code actual} something the consumer can live with, given that it expects {@code expected}?
     * <ul>
     * <li>every field the contract lists must be in the actual object, with a compatible value (recursively);</li>
     * <li>fields the contract does NOT list are allowed: adding a field is a compatible change;</li>
     * <li>arrays must have the same size and compatible elements, in order;</li>
     * <li>scalars must be equal, and of the same JSON type ({@code 5} is not {@code "5"}).</li>
     * </ul>
     */
    public static boolean compatible(JsonNode expected, JsonNode actual) {
        if (expected == null || actual == null) {
            return expected == actual;
        }
        if (expected.isObject()) {
            if (!actual.isObject()) {
                return false;
            }
            Iterator<Map.Entry<String, JsonNode>> fields = expected.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (!actual.has(field.getKey()) || !compatible(field.getValue(), actual.get(field.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        if (expected.isArray()) {
            if (!actual.isArray() || expected.size() != actual.size()) {
                return false;
            }
            for (int i = 0; i < expected.size(); i++) {
                if (!compatible(expected.get(i), actual.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return expected.getNodeType() == actual.getNodeType() && expected.equals(actual);
    }
}
