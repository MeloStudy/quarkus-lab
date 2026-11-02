package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * Task 1 and 2: the JSON contract. Every response must contain EXACTLY these properties: no more (the entity
 * has internal columns that must not leak), no fewer (clients depend on them). Property names, not values.
 */
@QuarkusTest
class ApiShapeTest {

    @Inject
    TestData data;

    @BeforeEach
    void reset() {
        data.reset();
    }

    @Test
    void aProductShowsExactlyItsPublicProperties() {
        given().when().get("/products/1")
                .then().statusCode(200)
                .body("keySet()", containsInAnyOrder("id", "name", "category", "price", "stock", "labels"));
    }

    @Test
    void theProductListShowsTheSameProperties() {
        given().when().get("/products")
                .then().statusCode(200)
                .body("[0].keySet()", containsInAnyOrder("id", "name", "category", "price", "stock", "labels"));
    }

    @Test
    void createAndUpdateAnswerWithTheSameShape() {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"Refactoring\",\"category\":\"books\",\"price\":42.5}")
                .when().post("/products")
                .then().statusCode(201)
                .body("keySet()", containsInAnyOrder("id", "name", "category", "price", "stock", "labels"));
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"Chess\",\"category\":\"games\",\"price\":25}")
                .when().put("/products/4")
                .then().statusCode(200)
                .body("keySet()", containsInAnyOrder("id", "name", "category", "price", "stock", "labels"));
        given().contentType(ContentType.JSON).body("[\"sale\"]")
                .when().put("/products/4/labels")
                .then().statusCode(200)
                .body("keySet()", containsInAnyOrder("id", "name", "category", "price", "stock", "labels"));
    }

    @Test
    void priceHistoryEntriesKeepTheirShape() {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"Chess\",\"category\":\"games\",\"price\":25}")
                .when().put("/products/4").then().statusCode(200);
        given().when().get("/products/4/price-history")
                .then().statusCode(200)
                .body("[0].keySet()", containsInAnyOrder("id", "productId", "oldPrice", "newPrice", "changedAt"));
    }

    @Test
    void reviewsKeepTheirShapeAndTheirTypeProperty() {
        given().contentType(ContentType.JSON)
                .body("{\"type\":\"CUSTOMER\",\"rating\":5,\"comment\":\"Great\",\"verifiedPurchase\":true}")
                .when().post("/products/1/reviews")
                .then().statusCode(201)
                .body("keySet()", containsInAnyOrder("type", "id", "rating", "comment", "createdAt", "verifiedPurchase"));
        given().contentType(ContentType.JSON)
                .body("{\"type\":\"CRITIC\",\"rating\":3,\"comment\":\"Fine\",\"outlet\":\"Daily News\"}")
                .when().post("/products/1/reviews")
                .then().statusCode(201)
                .body("keySet()", containsInAnyOrder("type", "id", "rating", "comment", "createdAt", "outlet"));
        given().when().get("/products/1/reviews")
                .then().statusCode(200)
                .body("[0].keySet()", containsInAnyOrder("type", "id", "rating", "comment", "createdAt", "outlet"))
                .body("[1].keySet()", containsInAnyOrder("type", "id", "rating", "comment", "createdAt", "verifiedPurchase"));
    }

    @Test
    void ordersKeepTheirShape() {
        int id = given().contentType(ContentType.JSON).body("{\"productId\":1,\"quantity\":1}")
                .when().post("/orders")
                .then().statusCode(201)
                .body("keySet()", containsInAnyOrder("id", "productId", "quantity", "total", "createdAt"))
                .extract().path("id");
        given().when().get("/orders/" + id)
                .then().statusCode(200)
                .body("keySet()", containsInAnyOrder("id", "productId", "quantity", "total", "createdAt"));
    }
}
