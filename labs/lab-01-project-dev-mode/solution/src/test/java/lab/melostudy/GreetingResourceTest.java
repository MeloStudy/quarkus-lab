package lab.melostudy;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class GreetingResourceTest {

    // Task 1: change the text of GET /hello (live coding in dev mode)
    @Test
    void helloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello from Quarkus"));
    }

    // Task 2: new endpoint GET /hello/{name}
    @Test
    void helloWithName() {
        given()
          .when().get("/hello/Melo")
          .then()
             .statusCode(200)
             .body(is("Hello, Melo!"));
    }

    // Task 3: add the smallrye-health extension with the Quarkus CLI / Maven
    @Test
    void healthEndpointIsUp() {
        given()
          .when().get("/q/health")
          .then()
             .statusCode(200)
             .body("status", is("UP"));
    }
}
