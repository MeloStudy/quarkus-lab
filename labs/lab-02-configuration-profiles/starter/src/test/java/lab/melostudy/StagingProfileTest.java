package lab.melostudy;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

/** Task 4: properties specific to the "staging" profile. */
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
          .then().body(is("Hello from staging, world!"));
    }

    @Test
    void appNameDiffersInStaging() {
        given().when().get("/info")
          .then().body(containsString("config-lab-staging"));
    }
}
