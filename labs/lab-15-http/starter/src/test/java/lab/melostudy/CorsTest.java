package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Task 3: a browser front-end on another origin. Only the origin we know, only the methods and headers it needs. */
@QuarkusTest
class CorsTest {

    private static final String FRONT_END = "http://localhost:3000";

    @Inject
    TestSupport support;

    @BeforeEach
    void reset() {
        support.reset();
    }

    @Test
    void thePreflightOfAnUpdateFromTheFrontEndIsAnswered() {
        given().header("Origin", FRONT_END)
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "if-match,content-type")
                .options("/api/v1/documents/1")
                .then().statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo(FRONT_END))
                .header("Access-Control-Allow-Methods", containsString("PUT"))
                .header("Access-Control-Allow-Headers", containsString("If-Match"));
    }

    @Test
    void methodsThatTheFrontEndDoesNotNeedAreNotAdvertised() {
        given().header("Origin", FRONT_END).header("Access-Control-Request-Method", "PUT")
                .options("/api/v1/documents/1")
                .then().header("Access-Control-Allow-Methods", containsString("PUT")); // CORS works at all
        given().header("Origin", FRONT_END).header("Access-Control-Request-Method", "DELETE")
                .options("/api/v1/documents/1")
                .then().header("Access-Control-Allow-Methods", not(containsString("DELETE")));
    }

    @Test
    void theFrontEndCanReadTheEtagAndTheLocationOfAResponse() {
        // Without Access-Control-Expose-Headers the browser hides every response header except a few "safe" ones,
        // so a front-end could never read the ETag it needs for If-Match.
        var created = given().header("Origin", FRONT_END).contentType("application/json")
                .body("{\"title\":\"Plan\",\"content\":\"v1\"}").post("/api/v1/documents");
        created.then().statusCode(201)
                .header("Access-Control-Allow-Origin", equalTo(FRONT_END))
                .header("Access-Control-Expose-Headers", containsString("ETag"))
                .header("Access-Control-Expose-Headers", containsString("Location"));
    }

    @Test
    void anotherOriginIsRefused() {
        given().header("Origin", "http://evil.example").get("/api/v1/documents/1")
                .then().statusCode(403).header("Access-Control-Allow-Origin", nullValue());
        given().header("Origin", "http://evil.example").header("Access-Control-Request-Method", "PUT")
                .options("/api/v1/documents/1")
                .then().statusCode(403);
    }

    @Test
    void requestsWithoutAnOriginAreNotAffected() {
        // curl, other services, same-origin pages: no Origin header, no CORS headers, no refusal
        given().get("/api/v1/documents/1").then().statusCode(404).header("Access-Control-Allow-Origin", nullValue());
    }
}
