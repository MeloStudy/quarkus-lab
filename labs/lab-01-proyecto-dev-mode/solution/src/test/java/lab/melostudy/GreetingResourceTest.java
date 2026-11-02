package lab.melostudy;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class GreetingResourceTest {

    // Tarea 1: cambiar el texto de GET /hello (live coding en dev mode)
    @Test
    void helloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hola desde Quarkus"));
    }

    // Tarea 2: nuevo endpoint GET /hello/{name}
    @Test
    void helloWithName() {
        given()
          .when().get("/hello/Melo")
          .then()
             .statusCode(200)
             .body(is("Hola, Melo!"));
    }

    // Tarea 3: agregar la extensión smallrye-health con la CLI de Quarkus / Maven
    @Test
    void healthEndpointIsUp() {
        given()
          .when().get("/q/health")
          .then()
             .statusCode(200)
             .body("status", is("UP"));
    }
}
