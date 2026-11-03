package lab.melostudy;

import static io.restassured.RestAssured.given;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

/** Shared by the tests: a clean desk before each test, and a way to place an order. */
abstract class DeskTestBase {

    @Inject
    Warehouse warehouse;

    @Inject
    Ledger ledger;

    @Inject
    DeadLetterStore deadLetters;

    @BeforeEach
    void cleanDesk() {
        warehouse.reset();
        ledger.reset();
        deadLetters.reset();
    }

    static Response place(String sku, int quantity) {
        return given().contentType(ContentType.JSON).body(Map.of("sku", sku, "quantity", quantity)).post("/orders");
    }
}
