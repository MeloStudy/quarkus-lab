package lab.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

/**
 * The orders service against a WireMock that plays the inventory service. Happy-path stubs come from the CONTRACT;
 * the failures a contract cannot describe (outages, slowness, garbage) are stubbed by hand.
 */
@QuarkusTest
@WithTestResource(WireMockResource.class)
class OrderFlowTest {

    @InjectWireMock
    WireMockServer wireMock;

    @BeforeEach
    void freshStubsAndJournal() {
        wireMock.resetAll(); // stubs AND the record of the requests received
    }

    private static ValidatableResponse order(String sku, int quantity) {
        return given().contentType(ContentType.JSON).body("{\"sku\":\"" + sku + "\",\"quantity\":" + quantity + "}")
                .when().post("/orders").then();
    }

    private static int orderCount() {
        return Integer.parseInt(given().get("/orders/count").asString());
    }

    // ---- the contract describes it: stubs generated from the contract

    @Test
    void anOrderWithStockIsConfirmedAndReservedInTheInventory() {
        ContractStubs.stub(wireMock, "stock of a known sku");
        ContractStubs.stub(wireMock, "reserve within the available stock");

        order("A1", 2).statusCode(201).body("status", equalTo("CONFIRMED")).body("quantity", equalTo(2));

        wireMock.verify(1, getRequestedFor(urlEqualTo("/stock/A1")));
        wireMock.verify(1, postRequestedFor(urlEqualTo("/stock/A1/reserve")).withRequestBody(equalToJson("{\"quantity\":2}")));
    }

    @Test
    void aConfirmedOrderCanBeReadBack() {
        ContractStubs.stub(wireMock, "stock of a known sku");
        ContractStubs.stub(wireMock, "reserve within the available stock");

        String location = order("A1", 2).statusCode(201).extract().header("Location");

        given().get(location).then().statusCode(200).body("sku", equalTo("A1")).body("status", equalTo("CONFIRMED"));
    }

    @Test
    void anUnknownProductIs422AndNothingIsReserved() {
        ContractStubs.stub(wireMock, "stock of an unknown sku");
        int before = orderCount();

        order("ZZ", 1).statusCode(422);

        wireMock.verify(0, postRequestedFor(urlMatching("/stock/.*/reserve")));
        assertEquals(before, orderCount());
    }

    @Test
    void notEnoughStockIsRefusedWithoutTryingToReserve() {
        ContractStubs.stub(wireMock, "stock of a known sku"); // 5 available
        int before = orderCount();

        order("A1", 9).statusCode(409);

        wireMock.verify(0, postRequestedFor(urlMatching("/stock/.*/reserve")));
        assertEquals(before, orderCount());
    }

    @Test
    void aSoldOutProductIs409() {
        ContractStubs.stub(wireMock, "stock of a sku that is sold out");

        order("B2", 1).statusCode(409);
    }

    // ---- what a contract cannot say: other people's bad days

    @Test
    void whenSomeoneElseTakesTheStockBetweenTheTwoCallsTheOrderIsRefused() {
        ContractStubs.stub(wireMock, "stock of a known sku"); // "5 available"...
        wireMock.stubFor(post(urlEqualTo("/stock/A1/reserve")) // ...but by the time we reserve, they are gone
                .willReturn(aResponse().withStatus(409).withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"INSUFFICIENT_STOCK\",\"available\":0}")));
        int before = orderCount();

        order("A1", 2).statusCode(409);

        assertEquals(before, orderCount());
    }

    @Test
    void anInventoryServerErrorIs502AndNoOrderIsCreated() {
        wireMock.stubFor(get(urlEqualTo("/stock/A1")).willReturn(aResponse().withStatus(500).withBody("boom")));
        int before = orderCount();

        order("A1", 1).statusCode(502);

        assertEquals(before, orderCount());
    }

    @Test
    void anAnswerThatIsNotJsonIs502() {
        wireMock.stubFor(get(urlEqualTo("/stock/A1")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "text/html").withBody("<html>maintenance</html>")));

        order("A1", 1).statusCode(502);
    }

    @Test
    void anAnswerWithoutTheFieldWeNeedIs502() {
        // 200 and valid JSON, but "available" is gone: exactly the kind of change a contract test would catch in the provider
        wireMock.stubFor(get(urlEqualTo("/stock/A1")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody("{\"sku\":\"A1\",\"stock\":5}")));

        order("A1", 1).statusCode(502);
    }

    @Test
    void aBrokenConnectionIs502() {
        wireMock.stubFor(get(urlEqualTo("/stock/A1")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        order("A1", 1).statusCode(502);
    }

    @Test
    void aSlowInventoryIs504AndWeDoNotWaitForIt() {
        wireMock.stubFor(get(urlEqualTo("/stock/A1")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody("{\"sku\":\"A1\",\"available\":5}")
                .withFixedDelay(3000)));

        long start = System.nanoTime();
        order("A1", 1).statusCode(504);
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(millis < 2500, "the 1 s timeout must cut the wait, but it took " + millis + " ms");
    }

    // ---- our own validation

    @Test
    void aMalformedOrderNeverReachesTheInventory() {
        order("A1", 0).statusCode(400);
        order(" ", 1).statusCode(400);

        assertTrue(wireMock.getAllServeEvents().isEmpty(), "the inventory service must not be called for invalid input");
    }
}
