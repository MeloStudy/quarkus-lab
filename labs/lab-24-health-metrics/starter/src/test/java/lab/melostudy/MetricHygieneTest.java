package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 4: metrics that stay useful when the application runs for months. */
@QuarkusTest
class MetricHygieneTest extends OpsTestBase {

    @Test
    void everyMetricCarriesTheNameOfTheApplication() {
        given().get("/api/orders/1").then().extract().statusCode();
        String metrics = Ops.metrics();
        long samples = metrics.lines().filter(line -> !line.startsWith("#") && !line.isBlank()).count();
        long tagged = metrics.lines().filter(line -> !line.startsWith("#") && !line.isBlank())
                .filter(line -> line.contains("application=\"orders\"")).count();
        assertThat("some metrics exist", samples > 10, equalTo(true));
        assertThat("all of them are tagged", tagged, equalTo(samples));
    }

    @Test
    void httpMetricsUseThePathTemplateNotTheRealPath() {
        for (int id = 1; id <= 5; id++) {
            given().get("/api/orders/" + id).then().extract().statusCode();
        }
        String metrics = Ops.metrics();
        assertThat(metrics, containsString("uri=\"/api/orders/{id}\""));
        assertThat("one series per template, not one per id", metrics, not(containsString("uri=\"/api/orders/3\"")));
    }

    @Test
    void theOrderMetricsHaveNoIdentifierAsATag() {
        given().contentType("application/json").body("{\"sku\":\"A-100\",\"quantity\":1,\"type\":\"standard\"}")
                .post("/api/orders").then().statusCode(201);
        String metrics = Ops.metrics();
        assertThat(metrics.lines().filter(line -> line.startsWith("orders_")).filter(line -> line.toLowerCase().contains("id=\"") || line.contains("sku=\"")).count(),
                equalTo(0L));
    }
}
