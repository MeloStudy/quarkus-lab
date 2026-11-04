package lab.shop;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/** The application can change what the library decided: its own configuration wins over the default of the library. */
@QuarkusTest
@TestProfile(LibraryConfigTest.TaxHoliday.class)
class LibraryConfigTest {

    public static class TaxHoliday implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("pricing.tax-rate", "0.10");
        }
    }

    @Test
    void theApplicationOverridesTheTaxRateOfTheLibrary() {
        given().get("/prices/total?quantity=2&unit=1000").then().statusCode(200).body(is("2200"));
    }
}
