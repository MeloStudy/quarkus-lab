package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Lab 10, tasks 2-3: an order is one unit of work, and the audit trail survives a rollback. */
@QuarkusTest
class OrderTest {

    @Inject
    TestData data;

    @BeforeEach
    void resetData() {
        data.reset();
    }

    private static io.restassured.response.ValidatableResponse order(Object productId, int quantity) {
        String id = productId == null ? "" : "\"productId\":" + productId + ",";
        return given().contentType(ContentType.JSON).body("{" + id + "\"quantity\":" + quantity + "}")
                .when().post("/orders").then();
    }

    // ---- Task 2: all or nothing

    @Test
    void anAcceptedOrderTakesTheStockAndIsRecorded() {
        int id = order(1, 2).statusCode(201)
                .body("productId", equalTo(1)).body("quantity", equalTo(2)).body("total", equalTo(61.0f))
                .extract().path("id");
        assertThat(data.stockOf(1), equalTo(8));
        assertThat(data.count("customer_order"), equalTo(1L));
        given().when().get("/orders/" + id).then().statusCode(200).body("quantity", equalTo(2));
    }

    @Test
    void notEnoughStockIsAConflictAndChangesNothing() {
        data.setStock(1, 3);
        order(1, 5).statusCode(409).body("code", equalTo("OUT_OF_STOCK"));
        assertThat(data.stockOf(1), equalTo(3));
        assertThat(data.count("customer_order"), equalTo(0L));
    }

    @Test
    void aRefusedPaymentUndoesTheStockAndTheOrder() {
        // 10 x 50.25 = 502.50, above the 500.00 payment limit. The stock was already taken and the order
        // already saved when the payment is refused: both must be rolled back.
        order(3, 10).statusCode(402).body("code", equalTo("PAYMENT_REFUSED"));
        assertThat(data.stockOf(3), equalTo(10));
        assertThat(data.count("customer_order"), equalTo(0L));
        // 9 x 50.25 = 452.25 is fine: the same product, the same route
        order(3, 9).statusCode(201);
        assertThat(data.stockOf(3), equalTo(1));
    }

    @Test
    void invalidOrdersAreRejectedBeforeAnyWork() {
        order(1, 1).statusCode(201); // the route works...
        order(1, 0).statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
        order(1, 51).statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
        order(null, 1).statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
        assertThat(data.count("customer_order"), equalTo(1L));
    }

    @Test
    void anUnknownProductIs404() {
        order(1, 1).statusCode(201); // the route works...
        order(999, 1).statusCode(404).body("code", equalTo("NOT_FOUND"));
        given().when().get("/orders/999").then().statusCode(404);
    }

    // ---- Task 3: the audit trail

    @Test
    void everyAttemptLeavesAnAuditEntryEvenWhenItIsRolledBack() {
        order(1, 1).statusCode(201);
        data.setStock(2, 1);
        order(2, 5).statusCode(409); // rolled back: out of stock
        order(3, 10).statusCode(402); // rolled back: payment refused
        assertThat(data.auditOutcomes(), contains("PLACED", "REJECTED_STOCK", "REJECTED_PAYMENT"));
        assertThat(data.count("customer_order"), equalTo(1L));
    }

    @Test
    void validationFailuresAreNotAudited() {
        order(1, 0).statusCode(400);
        order(999, 1).statusCode(404);
        assertThat(data.auditOutcomes(), empty());
    }
}
