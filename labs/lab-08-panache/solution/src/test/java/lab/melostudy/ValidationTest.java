package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;

/** Tasks 1 to 3: reject invalid input with 400 instead of accepting it or failing with 500. */
@QuarkusTest
class ValidationTest {

    @Inject
    TestData data;

    @BeforeEach
    void resetData() {
        data.reset();
    }

    private RequestSpecification json(String body) {
        return given().contentType(ContentType.JSON).body(body);
    }

    private static String product(String name, String category, String price) {
        return "{\"name\":" + name + ",\"category\":" + category + ",\"price\":" + price + "}";
    }

    // ---- Task 1: Bean Validation on the request body

    @Test
    void aValidProductIsStillCreated() {
        json(product("\"Kind of Blue\"", "\"music\"", "12.5"))
                .when().post("/products").then().statusCode(201);
    }

    @Test
    void blankNameIsRejected() {
        json(product("\"  \"", "\"books\"", "10")).when().post("/products").then().statusCode(400);
    }

    @Test
    void missingNameIsRejected() {
        json("{\"category\":\"books\",\"price\":10}").when().post("/products").then().statusCode(400);
    }

    @Test
    void zeroNegativeOrMissingPriceIsRejected() {
        json(product("\"A\"", "\"books\"", "0")).when().post("/products").then().statusCode(400);
        json(product("\"B\"", "\"books\"", "-5")).when().post("/products").then().statusCode(400);
        json("{\"name\":\"C\",\"category\":\"books\"}").when().post("/products").then().statusCode(400);
    }

    @Test
    void tooManyDecimalsOrTooLongNameIsRejected() {
        json(product("\"D\"", "\"books\"", "10.999")).when().post("/products").then().statusCode(400);
        json(product("\"" + "x".repeat(101) + "\"", "\"books\"", "10")).when().post("/products").then().statusCode(400);
    }

    @Test
    void anEmptyBodyIsA400NotA500() {
        given().contentType(ContentType.JSON).when().post("/products").then().statusCode(400);
    }

    @Test
    void updateIsValidatedToo() {
        json(product("\"Fine\"", "\"books\"", "10")).when().put("/products/1").then().statusCode(200);
        json(product("\"Fine\"", "\"books\"", "-1")).when().put("/products/1").then().statusCode(400);
    }

    @Test
    void rejectedInputIsNotStored() {
        json(product("\"\"", "\"books\"", "10")).when().post("/products").then().statusCode(400);
        given().when().get("/products").then().body("$", hasSize(5));
    }

    // ---- Task 2: validating query parameters

    @Test
    void pagingParametersAreValidated() {
        given().queryParam("page", -1).when().get("/products").then().statusCode(400);
        given().queryParam("size", 0).when().get("/products").then().statusCode(400);
        given().queryParam("size", 101).when().get("/products").then().statusCode(400);
        given().queryParam("size", 100).when().get("/products").then().statusCode(200);
    }

    // ---- Task 3: a custom constraint backed by a CDI bean

    @Test
    void onlyConfiguredCategoriesAreAccepted() {
        json(product("\"Sword\"", "\"weapons\"", "10")).when().post("/products").then().statusCode(400);
        json(product("\"Ok1\"", "\"BOOKS\"", "10")).when().post("/products").then().statusCode(201);
        json(product("\"Ok2\"", "\"games\"", "10")).when().post("/products").then().statusCode(201);
    }
}
