package lab.melostudy;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

/** Tarea 4: propiedades específicas del perfil "staging". */
@QuarkusTest
@TestProfile(StagingProfileTest.Staging.class)
class StagingProfileTest {

    public static class Staging implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "staging";
        }
    }

    @Test
    void greetingMessageDiffersInStaging() {
        given().when().get("/greeting")
          .then().body(is("Hola desde staging, mundo!"));
    }

    @Test
    void appNameDiffersInStaging() {
        given().when().get("/info")
          .then().body(containsString("config-lab-staging"));
    }
}
