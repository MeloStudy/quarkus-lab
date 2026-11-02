package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Task 4: listing, filtering and paging through query parameters. */
@QuarkusTest
class ProductQueryTest {

    @Inject
    ProductRepository repository;

    @BeforeEach
    void resetData() {
        repository.reset();
    }

    @Test
    void listReturnsEverythingByDefault() {
        given().when().get("/products")
                .then().statusCode(200)
                .body("$", hasSize(5))
                .header("X-Total-Count", "5");
    }

    @Test
    void filterByCategory() {
        given().queryParam("category", "games")
                .when().get("/products")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("category", everyItem(equalTo("games")))
                .header("X-Total-Count", "2");
    }

    @Test
    void firstPage() {
        given().queryParam("page", 0).queryParam("size", 2)
                .when().get("/products")
                .then().statusCode(200)
                .body("id", contains(1, 2))
                .header("X-Total-Count", "5");
    }

    @Test
    void lastPageMayBeShorter() {
        given().queryParam("page", 2).queryParam("size", 2)
                .when().get("/products")
                .then().statusCode(200)
                .body("id", contains(5))
                .header("X-Total-Count", "5");
    }

    @Test
    void pageBeyondTheEndIsAnEmptyListNotAnError() {
        given().queryParam("page", 10).queryParam("size", 2)
                .when().get("/products")
                .then().statusCode(200)
                .body("$", hasSize(0))
                .header("X-Total-Count", "5");
    }

    @Test
    void filterAndPagingCombine() {
        given().queryParam("category", "books").queryParam("page", 1).queryParam("size", 2)
                .when().get("/products")
                .then().statusCode(200)
                .body("id", contains(3))
                .header("X-Total-Count", "3");
    }
}
