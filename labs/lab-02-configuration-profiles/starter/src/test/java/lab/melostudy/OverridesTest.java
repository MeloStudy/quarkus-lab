package lab.melostudy;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

/** Tasks 1 and 3: the code must read the configuration, not hardcode values. */
@QuarkusTest
@TestProfile(OverridesTest.CustomConfig.class)
class OverridesTest {

    public static class CustomConfig implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "greeting.message", "Hello",
                "greeting.name", "Melo",
                "greeting.punctuation", "?",
                "api.host", "example.org",
                "app.version", "2.0.0");
        }
    }

    @Test
    void greetingReadsConfiguration() {
        given().when().get("/greeting")
          .then().statusCode(200).body(is("Hello, Melo?"));
    }

    @Test
    void infoReadsOverriddenValues() {
        given().when().get("/info")
          .then()
          .body(containsString("2.0.0"))
          .body(containsString("https://example.org/api"));
    }
}
