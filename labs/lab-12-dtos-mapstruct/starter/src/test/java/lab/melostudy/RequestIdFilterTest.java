package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Lab 04, task 5: a response filter that adds an X-Request-Id header to every response. */
@QuarkusTest
class RequestIdFilterTest {

    @Test
    void generatesAnIdWhenTheClientSendsNone() {
        given().when().get("/products/1")
                .then().header("X-Request-Id", not(emptyOrNullString()));
    }

    @Test
    void echoesTheIdSentByTheClient() {
        given().header("X-Request-Id", "abc-123")
                .when().get("/products/1")
                .then().header("X-Request-Id", equalTo("abc-123"));
    }

    @Test
    void alsoPresentOnErrorResponses() {
        given().when().get("/products/999")
                .then().statusCode(404)
                .header("X-Request-Id", not(emptyOrNullString()));
    }
}
