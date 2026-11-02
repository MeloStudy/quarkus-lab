package lab.melostudy;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

/** Tasks 2 and 3: @ConfigMapping, default values and property expressions. */
@QuarkusTest
class InfoResourceTest {

    @Test
    void infoShowsMappedConfiguration() {
        given().when().get("/info")
          .then().statusCode(200)
          .body(containsString("config-lab"))                 // app.name
          .body(containsString("quarkus,config,lab"))         // app.tags (List<String>)
          .body(containsString("dev@lab.melostudy"));         // app.contact.email (nested group)
    }

    @Test
    void versionComesFromDefaultValue() {
        given().when().get("/info")
          .then().body(containsString("0.1.0"));              // @WithDefault: app.version is NOT in the file
    }

    @Test
    void baseUrlUsesPropertyExpression() {
        given().when().get("/info")
          .then().body(containsString("https://localhost/api")); // ${api.host:localhost}
    }
}
