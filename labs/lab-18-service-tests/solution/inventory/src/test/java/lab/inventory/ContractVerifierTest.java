package lab.inventory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Task 4: the rule that decides whether the provider keeps its promise. A plain unit test: no Quarkus. A verifier that
 * says "yes" to everything would make every contract test green, so the verifier itself is tested.
 */
class ContractVerifierTest {

    private static JsonNode json(String text) {
        try {
            return new ObjectMapper().readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static boolean compatible(String expected, String actual) {
        return ContractVerifier.compatible(json(expected), json(actual));
    }

    @Test
    void identicalDocumentsAreCompatible() {
        assertTrue(compatible("{\"sku\":\"A1\",\"available\":5}", "{\"sku\":\"A1\",\"available\":5}"));
    }

    @Test
    void anExtraFieldInTheActualResponseIsACompatibleChange() {
        assertTrue(compatible("{\"sku\":\"A1\"}", "{\"sku\":\"A1\",\"warehouse\":\"north\"}"));
    }

    @Test
    void aMissingFieldBreaksTheContract() {
        assertFalse(compatible("{\"sku\":\"A1\",\"available\":5}", "{\"sku\":\"A1\"}"));
    }

    @Test
    void aRenamedFieldBreaksTheContract() {
        assertFalse(compatible("{\"sku\":\"A1\",\"available\":5}", "{\"sku\":\"A1\",\"stock\":5}"));
    }

    @Test
    void aDifferentValueBreaksTheContract() {
        assertFalse(compatible("{\"available\":5}", "{\"available\":4}"));
    }

    @Test
    void aDifferentTypeBreaksTheContractEvenIfTheTextLooksEqual() {
        assertFalse(compatible("{\"available\":5}", "{\"available\":\"5\"}"));
    }

    @Test
    void nestedObjectsAreComparedRecursively() {
        assertTrue(compatible("{\"a\":{\"b\":1}}", "{\"a\":{\"b\":1,\"c\":2}}"));
        assertFalse(compatible("{\"a\":{\"b\":1}}", "{\"a\":{\"b\":2}}"));
        assertFalse(compatible("{\"a\":{\"b\":1}}", "{\"a\":1}"));
    }

    @Test
    void arraysNeedTheSameSizeAndCompatibleElementsInOrder() {
        assertTrue(compatible("[1,2]", "[1,2]"));
        assertFalse(compatible("[1,2]", "[1,2,3]"));
        assertFalse(compatible("[1,2]", "[2,1]"));
        assertTrue(compatible("[{\"a\":1}]", "[{\"a\":1,\"b\":2}]"));
    }

    @Test
    void nullIsAValueNotAnAbsence() {
        assertFalse(compatible("{\"error\":null}", "{}"));
        assertTrue(compatible("{\"error\":null}", "{\"error\":null}"));
        assertFalse(compatible("{\"error\":null}", "{\"error\":\"x\"}"));
    }
}
