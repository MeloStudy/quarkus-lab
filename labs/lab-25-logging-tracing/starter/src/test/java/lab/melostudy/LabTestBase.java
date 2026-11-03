package lab.melostudy;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** The fake stock service is started once for the whole run. SKU {@code SOLD-OUT} answers 409 and {@code BROKEN} answers 500. */
@QuarkusTestResource(StockStub.class)
abstract class LabTestBase {

    static final String EMAIL = "ana@example.com";

    static RequestSpecification request() {
        return given().contentType(ContentType.JSON);
    }

    static String body(String sku) {
        return "{\"sku\":\"" + sku + "\",\"quantity\":2,\"customerEmail\":\"" + EMAIL + "\"}";
    }

    static Response order(String sku) {
        return request().body(body(sku)).post("/api/orders");
    }

    static Response order(String sku, String requestId) {
        return request().header("X-Request-Id", requestId).body(body(sku)).post("/api/orders");
    }
}
