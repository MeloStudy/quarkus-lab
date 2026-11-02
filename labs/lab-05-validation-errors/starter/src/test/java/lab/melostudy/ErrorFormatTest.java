package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;

/** Tasks 4 and 5: every failure uses the same JSON shape ({@link ApiError}) and leaks nothing. */
@QuarkusTest
class ErrorFormatTest {

    @Inject
    ProductRepository repository;

    @BeforeEach
    void resetData() {
        repository.reset();
    }

    private RequestSpecification json(String body) {
        return given().contentType(ContentType.JSON).body(body);
    }

    // ---- Task 4: validation, wrong-shape JSON and business errors

    @Test
    void validationErrorsListTheOffendingFields() {
        json("{\"name\":\"\",\"category\":\"books\",\"price\":-1}")
                .when().post("/products")
                .then().statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("message", not(emptyOrNullString()))
                .body("errors.field", hasItem("name"))
                .body("errors.field", hasItem("price"))
                .body("errors.message", not(hasItem(emptyOrNullString())));
    }

    @Test
    void fieldNamesAreTheJsonNamesNotJavaInternals() {
        json("{\"name\":\"ok\",\"category\":\"books\",\"price\":-1}")
                .when().post("/products")
                .then().statusCode(400)
                .body("errors.field", hasItem("price"))
                .body(not(containsString("create.")))
                .body(not(containsString("arg0")));
    }

    @Test
    void parameterViolationsUseTheParameterName() {
        given().queryParam("page", -1)
                .when().get("/products")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("errors.field", hasItem("page"));
    }

    @Test
    void wrongTypeInsideTheJsonIsAlsoMalformed() {
        json("{\"name\":\"a\",\"category\":\"books\",\"price\":\"abc\"}")
                .when().post("/products")
                .then().statusCode(400)
                .body("code", equalTo("MALFORMED_REQUEST"))
                .body(not(containsString("objectName")));
    }

    @Test
    void duplicateNameIsAConflict() {
        json("{\"name\":\"clean code\",\"category\":\"books\",\"price\":10}")
                .when().post("/products")
                .then().statusCode(409)
                .contentType(ContentType.JSON)
                .body("code", equalTo("CONFLICT"))
                .body("message", containsString("clean code"));
    }

    @Test
    void updatingToATakenNameIsAConflictButKeepingYourOwnIsFine() {
        json("{\"name\":\"Chess\",\"category\":\"games\",\"price\":10}")
                .when().put("/products/4").then().statusCode(200);
        json("{\"name\":\"Catan\",\"category\":\"games\",\"price\":10}")
                .when().put("/products/4").then().statusCode(409);
    }

    // ---- Task 5: HTTP errors and the catch-all

    @Test
    void notFoundUsesTheSameShape() {
        given().when().get("/products/999")
                .then().statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", not(emptyOrNullString()));
    }

    @Test
    void malformedJsonIsA400WithoutJacksonInternals() {
        json("{oops")
                .when().post("/products")
                .then().statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("MALFORMED_REQUEST"))
                .body(not(containsString("objectName")))
                .body(not(containsString("column")));
    }

    @Test
    void otherHttpErrorsKeepTheirStatusButGetTheSameShape() {
        given().contentType(ContentType.TEXT).body("plain")
                .when().post("/products")
                .then().statusCode(415)
                .contentType(ContentType.JSON)
                .body("code", equalTo("HTTP_415"));
        given().when().delete("/products")
                .then().statusCode(405)
                .body("code", equalTo("HTTP_405"));
    }

    @Test
    void unexpectedFailuresAreA500ThatLeaksNothing() {
        given().when().get("/test-support/boom")
                .then().statusCode(500)
                .contentType(ContentType.JSON)
                .body("code", equalTo("INTERNAL_ERROR"))
                .body("errorId", notNullValue())
                .body(not(containsString("secret")))
                .body(not(containsString("jdbc")))
                .body(not(containsString("IllegalState")));
    }

    @Test
    void theRequestIdHeaderIsPresentEvenOnA500() {
        given().header("X-Request-Id", "trace-42")
                .when().get("/test-support/boom")
                .then().statusCode(500)
                .header("X-Request-Id", "trace-42");
    }
}
