package lab.shop;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Task 1: the endpoint, the beans and the interceptor of the library are part of the application. All of them live in another
 * jar: Quarkus finds them only if that jar has an index.
 */
@QuarkusTest
class DiscoveryTest {

    private static int calls() {
        return Integer.parseInt(given().get("/prices/calls").then().statusCode(200).extract().asString());
    }

    @Test
    void theApplicationServesTheEndpointOfTheLibrary() {
        given().get("/prices/total?quantity=3&unit=100").then().statusCode(200).body(is("360"));
    }

    @Test
    void theInterceptorOfTheLibraryWrapsABeanOfTheApplication() {
        int before = calls();

        given().get("/orders/checkout?customer=ana").then().statusCode(200).body(is("checked out ana"));

        assertEquals(before + 1, calls(), "the @Logged interceptor of the library did not run");
    }

    @Test
    void theApplicationOwnEndpointsKeepWorking() {
        given().get("/orders/checkout?customer=bob").then().statusCode(200).body(is("checked out bob"));
    }
}
