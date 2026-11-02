package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Regression guard from lab 04: the CRUD behavior must keep working. */
@QuarkusTest
class ProductResourceTest {

    @Inject
    ProductRepository repository;

    @BeforeEach
    void resetData() {
        repository.reset();
    }

    // ---- (lab 04) Task 1: GET /products/{id}

    @Test
    void getReturnsTheProductAsJson() {
        given().when().get("/products/1")
                .then().statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("name", equalTo("Clean Code"))
                .body("category", equalTo("books"))
                .body("price", equalTo(30.5f));
    }

    @Test
    void getUnknownProductIs404() {
        given().when().get("/products/1").then().statusCode(200); // the route exists...
        given().when().get("/products/999").then().statusCode(404); // ...but this id does not
    }

    // ---- (lab 04) Task 2: POST /products

    @Test
    void createReturns201WithLocationAndBody() {
        String location = given().contentType(ContentType.JSON)
                .body("{\"name\":\"Refactoring\",\"category\":\"books\",\"price\":42.0}")
                .when().post("/products")
                .then().statusCode(201)
                .header("Location", endsWith("/products/6"))
                .body("id", equalTo(6))
                .body("name", equalTo("Refactoring"))
                .extract().header("Location");

        // the Location header must be a working URL for the new resource
        given().when().get(location).then().statusCode(200).body("name", equalTo("Refactoring"));
    }

    @Test
    void createWithAnotherContentTypeIs415() {
        given().contentType(ContentType.TEXT).body("not json")
                .when().post("/products")
                .then().statusCode(415);
    }

    // ---- (lab 04) Task 3: PUT and DELETE

    @Test
    void updateReplacesTheProduct() {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"Chess Deluxe\",\"category\":\"games\",\"price\":99.5}")
                .when().put("/products/4")
                .then().statusCode(200)
                .body("id", equalTo(4))
                .body("name", equalTo("Chess Deluxe"))
                .body("price", equalTo(99.5f));

        given().when().get("/products/4").then().body("name", equalTo("Chess Deluxe"));
    }

    @Test
    void updateUnknownProductIs404() {
        String body = "{\"name\":\"Placeholder\",\"category\":\"books\",\"price\":1}";
        given().contentType(ContentType.JSON).body(body).when().put("/products/1").then().statusCode(200);
        given().contentType(ContentType.JSON).body(body).when().put("/products/999").then().statusCode(404);
    }

    @Test
    void deleteReturns204AndTheProductIsGone() {
        given().when().delete("/products/3").then().statusCode(204);
        given().when().get("/products/3").then().statusCode(404);
    }

    @Test
    void deletingTwiceReturns404TheSecondTime() {
        given().when().delete("/products/5").then().statusCode(204);
        given().when().delete("/products/5").then().statusCode(404);
    }

    @Test
    void errorResponsesDoNotLeakInternals() {
        given().when().get("/products/1").then().statusCode(200);
        given().when().get("/products/999")
                .then().statusCode(404)
                .body(org.hamcrest.Matchers.not(containsString("Exception")));
    }
}
