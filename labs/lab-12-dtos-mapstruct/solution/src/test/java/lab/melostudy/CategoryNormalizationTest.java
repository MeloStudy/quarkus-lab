package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * Task 3 (second half): the new constraint turns a sloppy habit of the application into an error.
 * The API accepts categories in any case ("Books", " GAMES "); the application must now store them
 * normalized, otherwise the database refuses the row.
 */
@QuarkusTest
class CategoryNormalizationTest {

    @Inject
    TestData data;

    @BeforeEach
    void reset() {
        data.reset();
    }

    @Test
    void createStoresTheCategoryNormalized() {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"Refactoring\",\"category\":\"  Books \",\"price\":42.5}")
                .when().post("/products")
                .then().statusCode(201).body("category", equalTo("books"));
    }

    @Test
    void updateStoresTheCategoryNormalized() {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"Chess\",\"category\":\"GAMES\",\"price\":21}")
                .when().put("/products/4")
                .then().statusCode(200).body("category", equalTo("games"));
        given().when().get("/products/4").then().statusCode(200).body("category", equalTo("games"));
    }
}
