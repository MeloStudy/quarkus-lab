package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 1: the order goes through the channels, and the answer to the sender depends on what the channels say back. */
@QuarkusTest
@Order(1)
class PipelineTest extends DeskTestBase {

    @Test
    void aValidOrderIsFulfilled() {
        place("A-100", 2).then().statusCode(201).body("status", is("FULFILLED"));
    }

    @Test
    void theAnswerPointsToTheOrderAndTheLedgerKnowsIt() {
        String id = place("B-200", 1).then().statusCode(201).extract().path("id");

        given().get("/orders/" + id).then().statusCode(200).body("sku", is("B-200")).body("quantity", is(1)).body("status", is("FULFILLED"));
        place("B-200", 1).then().statusCode(201).header("Location", not(endsWith("/orders/" + id)));
    }

    @Test
    void anUnknownOrderIsNotInTheLedger() {
        place("A-100", 1).then().statusCode(201);

        given().get("/orders/nobody").then().statusCode(404);
    }

    @Test
    void aWrongQuantityIsRefusedBeforeTheWarehouse() {
        place("A-100", 1).then().statusCode(201);
        warehouse.reset();
        ledger.reset();

        place("A-100", 0).then().statusCode(422).body("code", is("INVALID_ORDER"));
        place("A-100", -3).then().statusCode(422).body("code", is("INVALID_ORDER"));

        assertEquals(0, warehouse.calls(), "a refused order must not reach the warehouse");
        assertEquals(0, ledger.size(), "a refused order is not in the ledger");
    }

    @Test
    void anUnknownSkuIsRefusedBeforeTheWarehouse() {
        place("A-100", 1).then().statusCode(201);
        warehouse.reset();

        place("Z-999", 1).then().statusCode(422).body("code", is("INVALID_ORDER"));

        assertEquals(0, warehouse.calls());
    }

    @Test
    void theChannelKeepsWorkingAfterARefusal() {
        place("Z-999", 1).then().statusCode(422);

        place("A-100", 1).then().statusCode(201);
    }
}
