package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Lab 10, task 1: dynamic queries and aggregates computed by the database. */
@QuarkusTest
class SearchAndReportTest {

    @Inject
    TestData data;

    @BeforeEach
    void resetData() {
        data.reset();
    }

    @Test
    void textSearchIsACaseInsensitiveSubstringOfTheName() {
        given().queryParam("text", "CODE").when().get("/products")
                .then().statusCode(200).header("X-Total-Count", "1").body("id", contains(1));
        given().queryParam("text", "e").when().get("/products")
                .then().header("X-Total-Count", "4").body("id", contains(1, 2, 3, 4)); // every name but "Catan"
    }

    @Test
    void wildcardsTypedByTheUserMeanThemselves() {
        // no product name contains a percent sign or an underscore
        given().queryParam("text", "%").when().get("/products")
                .then().statusCode(200).header("X-Total-Count", "0").body("$", hasSize(0));
        given().queryParam("text", "_").when().get("/products")
                .then().header("X-Total-Count", "0");
    }

    @Test
    void priceRangeIsInclusiveAndCombinesWithTheOtherFilters() {
        given().queryParam("minPrice", "40").when().get("/products")
                .then().header("X-Total-Count", "3").body("id", contains(2, 3, 5));
        given().queryParam("minPrice", "30.50").queryParam("maxPrice", "40.50").when().get("/products")
                .then().header("X-Total-Count", "2").body("id", contains(1, 5));
        given().queryParam("minPrice", "40").queryParam("category", "games").when().get("/products")
                .then().header("X-Total-Count", "1").body("id", contains(5));
        given().queryParam("minPrice", "40").queryParam("size", 2).queryParam("page", 1).when().get("/products")
                .then().header("X-Total-Count", "3").body("id", contains(5));
    }

    @Test
    void invalidFiltersAreRejected() {
        given().queryParam("minPrice", "-1").when().get("/products")
                .then().statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void theReportAggregatesInTheDatabase() {
        given().when().get("/reports/categories")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("category", contains("books", "games"))
                .body("products", contains(3, 2))
                .body("stock", contains(30, 20))
                .body("averagePrice", contains(41.92f, 30.25f));
    }

    @Test
    void theReportFollowsTheData() {
        data.setStock(4, 0);
        given().when().get("/products/4").then().body("stock", equalTo(0));
        given().when().get("/reports/categories")
                .then().body("stock", contains(30, 10));
    }
}
