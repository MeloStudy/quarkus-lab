package lab.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;

/**
 * Task 5: the same service, but as the PACKAGED application: {@code mvn verify} builds the jar, starts it as
 * {@code java -jar}, and these tests talk to it over HTTP. The test class lives in another process: it cannot
 * {@code @Inject} anything of the application, it cannot use {@code @InjectMock} or {@code QuarkusMock}. What it
 * can use is what a client uses (HTTP), plus the test resource, which still starts WireMock and gives the
 * application its URL.
 */
@QuarkusIntegrationTest
@WithTestResource(WireMockResource.class)
class OrdersIT {

    @InjectWireMock
    WireMockServer wireMock;

    @BeforeEach
    void freshStubs() {
        wireMock.resetAll();
    }

    @Test
    void thePackagedApplicationConfirmsAnOrder() {
        ContractStubs.stub(wireMock, "stock of a known sku");
        ContractStubs.stub(wireMock, "reserve within the available stock");

        given().contentType(ContentType.JSON).body("{\"sku\":\"A1\",\"quantity\":2}")
                .when().post("/orders").then().statusCode(201).body("status", equalTo("CONFIRMED"));

        wireMock.verify(1, getRequestedFor(urlEqualTo("/stock/A1")));
    }

    @Test
    void thePackagedApplicationTranslatesAnInventoryFailure() {
        wireMock.stubFor(get(urlEqualTo("/stock/A1")).willReturn(aResponse().withStatus(500)));

        given().contentType(ContentType.JSON).body("{\"sku\":\"A1\",\"quantity\":1}")
                .when().post("/orders").then().statusCode(502);
    }
}
