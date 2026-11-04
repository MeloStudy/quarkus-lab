package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingResourceTest {

    @Test
    void answersWithTheConfiguredPrefix() {
        given().when().get("/greeting/Ana").then().statusCode(200).body(is("Hello, Ana!"));
    }
}
