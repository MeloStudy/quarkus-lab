package lab.melostudy;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

/** Default configuration (test profile, no overrides). It must stay green throughout the lab. */
@QuarkusTest
class GreetingResourceTest {

    @Test
    void greetingUsesDefaults() {
        given().when().get("/greeting")
          .then().statusCode(200).body(is("Hello, world!"));
    }
}
