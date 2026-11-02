package lab.inventory;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

/**
 * Task 4 (provider side): replays EVERY interaction of the contract against the real inventory service and checks the
 * answer. If this service changes something the orders service relies on, this test fails here, before anyone deploys.
 */
@QuarkusTest
class ProviderContractTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Inject
    StockRepository repository;

    @TestFactory
    Stream<DynamicTest> theProviderKeepsEveryPromiseOfTheContract() {
        return Contract.load().stream().map(interaction -> DynamicTest.dynamicTest(interaction.name(), () -> verify(interaction)));
    }

    @TestFactory
    Stream<DynamicTest> theContractIsNotEmpty() {
        List<Contract.Interaction> interactions = Contract.load();
        return Stream.of(DynamicTest.dynamicTest("the contract has interactions", () -> assertTrue(interactions.size() >= 6)));
    }

    private void verify(Contract.Interaction interaction) throws Exception {
        // PROVIDER STATE: the world the interaction assumes (this unit is "given A1 has 5 and B2 has 0")
        repository.reset();
        Iterator<Map.Entry<String, JsonNode>> given = interaction.given().fields();
        while (given.hasNext()) {
            Map.Entry<String, JsonNode> entry = given.next();
            repository.set(entry.getKey(), entry.getValue().asInt());
        }

        var request = given().contentType(ContentType.JSON);
        if (interaction.request().body() != null) {
            request = request.body(interaction.request().body().toString());
        }
        Response response = "GET".equals(interaction.request().method())
                ? request.get(interaction.request().path())
                : request.post(interaction.request().path());

        assertEquals(interaction.response().status(), response.statusCode(), interaction.name() + ": status");
        JsonNode actual = JSON.readTree(response.asString());
        assertTrue(ContractVerifier.compatible(interaction.response().body(), actual),
                interaction.name() + ": expected " + interaction.response().body() + " but the provider answered " + actual);
    }
}
