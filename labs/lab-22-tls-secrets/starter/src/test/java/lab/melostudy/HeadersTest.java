package lab.melostudy;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 2: headers that tell the browser how to treat what it received. They go on every response. */
@QuarkusTest
class HeadersTest {

    @Test
    void successfulResponsesCarryTheSecurityHeaders() {
        Https.request().get("/api/hello").then()
                .statusCode(200)
                .header("X-Content-Type-Options", equalTo("nosniff"))
                .header("X-Frame-Options", equalTo("DENY"))
                .header("Referrer-Policy", equalTo("no-referrer"))
                .header("Strict-Transport-Security", equalTo("max-age=31536000; includeSubDomains"))
                .header("Content-Security-Policy", equalTo("default-src 'none'; frame-ancestors 'none'"))
                .header("Cache-Control", equalTo("no-store"));
    }

    @Test
    void errorResponsesCarryThemToo() {
        Https.request().get("/api/hello").then().statusCode(200);
        Https.request().get("/api/does-not-exist").then()
                .statusCode(404)
                .header("X-Content-Type-Options", equalTo("nosniff"))
                .header("Cache-Control", equalTo("no-store"));
    }

    @Test
    void theDataOfThePartnerEndpointIsNeverCached() {
        Https.request().header("X-Api-Key", "test-only-key").get("/api/partner/data").then()
                .statusCode(200)
                .header("Cache-Control", equalTo("no-store"));
    }
}
