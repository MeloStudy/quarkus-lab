package lab.melostudy;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

/** Tareas 2 y 3: @ConfigMapping, valores por defecto y expresiones de propiedades. */
@QuarkusTest
class InfoResourceTest {

    @Test
    void infoShowsMappedConfiguration() {
        given().when().get("/info")
          .then().statusCode(200)
          .body(containsString("config-lab"))                 // app.name
          .body(containsString("quarkus,config,lab"))         // app.tags (List<String>)
          .body(containsString("dev@lab.melostudy"));         // app.contact.email (grupo anidado)
    }

    @Test
    void versionComesFromDefaultValue() {
        given().when().get("/info")
          .then().body(containsString("0.1.0"));              // @WithDefault: app.version NO está en el archivo
    }

    @Test
    void baseUrlUsesPropertyExpression() {
        given().when().get("/info")
          .then().body(containsString("https://localhost/api")); // ${api.host:localhost}
    }
}
