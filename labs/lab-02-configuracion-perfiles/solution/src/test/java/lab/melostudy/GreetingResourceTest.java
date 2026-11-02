package lab.melostudy;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

/** Configuración por defecto (perfil test, sin overrides). Debe seguir en verde durante todo el lab. */
@QuarkusTest
class GreetingResourceTest {

    @Test
    void greetingUsesDefaults() {
        given().when().get("/greeting")
          .then().statusCode(200).body(is("Hola, mundo!"));
    }
}
