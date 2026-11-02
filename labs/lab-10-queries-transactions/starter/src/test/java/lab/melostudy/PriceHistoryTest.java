package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Lab 08, task 4: price changes are recorded (active record entity) in the same transaction as the update. */
@QuarkusTest
class PriceHistoryTest {

    @Inject
    TestData data;

    @BeforeEach
    void resetData() {
        data.reset();
    }

    private static void put(int id, String name, String price) {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "\",\"category\":\"books\",\"price\":" + price + "}")
                .when().put("/products/" + id)
                .then().statusCode(200);
    }

    @Test
    void aNewProductHasNoHistory() {
        given().when().get("/products/1/price-history")
                .then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void everyPriceChangeIsRecordedNewestFirst() {
        put(1, "Clean Code", "35.00");
        put(1, "Clean Code", "40.00");
        given().when().get("/products/1/price-history")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("[0].oldPrice", equalTo(35.0f))
                .body("[0].newPrice", equalTo(40.0f))
                .body("[1].oldPrice", equalTo(30.5f))
                .body("[1].newPrice", equalTo(35.0f))
                .body("[0].productId", equalTo(1));
    }

    @Test
    void updatesThatKeepThePriceRecordNothing() {
        put(1, "Clean Code (2nd ed.)", "30.50");
        given().when().get("/products/1/price-history").then().body("$", hasSize(0));
    }

    @Test
    void historyOfAnUnknownProductIs404() {
        given().when().get("/products/1/price-history").then().statusCode(200); // the route exists...
        given().when().get("/products/999/price-history")
                .then().statusCode(404).body("code", equalTo("NOT_FOUND"));
    }

    @Test
    void deletingAProductDeletesItsHistory() {
        put(2, "Effective Java", "50.00");
        given().when().delete("/products/2").then().statusCode(204);
        given().when().get("/products/2/price-history").then().statusCode(404);
        // a new product reusing the freed name starts clean
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"Effective Java\",\"category\":\"books\",\"price\":10}")
                .when().post("/products").then().statusCode(201);
    }
}
