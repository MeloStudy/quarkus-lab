package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class EndpointsTest {

    @Test
    void aPluginIsLoadedByName() {
        given().when().get("/plugins/English").then().statusCode(200).body(equalTo("Hello"));
        given().when().get("/plugins/Spanish").then().statusCode(200).body(equalTo("Hola"));
    }

    @Test
    void anUnknownPluginIsNotFound() {
        given().when().get("/plugins/German").then().statusCode(404);
    }

    @Test
    void theRatesFileIsServed() {
        given().when().get("/rates").then().statusCode(200).body(containsString("\"USD\""));
    }
}
