package lab.inventory;

import com.fasterxml.jackson.databind.JsonNode;

/** The comparison rule of the provider verification. */
public final class ContractVerifier {

    private ContractVerifier() {
    }

    /**
     * Is {@code actual} something the consumer can live with, given that it expects {@code expected}?
     * TODO task 4: today this says "yes" to everything, so every contract test is green. Implement the rule:
     * every field the contract lists must be in the actual object with a compatible value (recursively); fields the
     * contract does NOT list are allowed (adding a field is a compatible change); arrays need the same size and
     * compatible elements in order; scalars must be equal AND of the same JSON type (5 is not "5").
     */
    public static boolean compatible(JsonNode expected, JsonNode actual) {
        return true;
    }
}
