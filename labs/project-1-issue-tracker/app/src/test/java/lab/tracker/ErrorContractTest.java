package lab.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Whatever goes wrong, the client sees the same shape and learns nothing about the inside. */
@QuarkusTest
class ErrorContractTest {

    @Test
    void anUnexpectedFailureIsAGenericBodyWithAnIdAndNoDetails() {
        given().get("/test-support/boom").then().statusCode(500)
                .body("code", equalTo("INTERNAL_ERROR"), "errorId", notNullValue())
                .body(not(containsString("secret")), not(containsString("jdbc")), not(containsString("Exception")));
    }

    @Test
    void routingFailuresUseTheSameShape() {
        given().get("/api/nothing-here").then().statusCode(404).body("code", equalTo("NOT_FOUND"));
        given().delete("/api/projects").then().statusCode(405).body("code", equalTo("HTTP_405"));
        given().contentType(ContentType.TEXT).body("TRK").post("/api/projects").then().statusCode(415)
                .body("code", equalTo("HTTP_415"));
    }
}
