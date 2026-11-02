package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;

/** Task 5: a new representation next to the old one, and a clear message to the clients of the old one. */
@QuarkusTest
class VersioningTest {

    @Inject
    TestSupport support;

    @BeforeEach
    void reset() {
        support.reset();
    }

    @Test
    void version1KeepsItsShapeAndAnnouncesItsEnd() {
        Response created = TestSupport.createDocument("Plan", "v1");
        given().get(TestSupport.urlOf(created)).then().statusCode(200)
                .body("keySet()", containsInAnyOrder("id", "title", "content", "version"))
                .header("Deprecation", equalTo("true"))
                .header("Sunset", notNullValue());
    }

    @Test
    void theDeprecationAlsoReachesErrorsAndCreations() {
        TestSupport.createDocument("Plan", "v1").then().header("Deprecation", equalTo("true"));
        given().get("/api/v1/documents/999999").then().statusCode(404).header("Deprecation", equalTo("true"));
    }

    @Test
    void version2HasANewShapeAndNoDeprecationNotice() {
        Response created = TestSupport.createDocument("Plan", "v1");
        long id = created.jsonPath().getLong("id");

        given().get("/api/v2/documents/" + id).then().statusCode(200)
                .body("keySet()", containsInAnyOrder("id", "name", "content"))
                .body("name", equalTo("Plan"))
                .header("Deprecation", nullValue())
                .header("Sunset", nullValue());
    }

    @Test
    void bothVersionsShareTheSameValidators() {
        Response created = TestSupport.createDocument("Plan", "v1");
        long id = created.jsonPath().getLong("id");

        String v2Etag = given().get("/api/v2/documents/" + id).getHeader("ETag");
        org.junit.jupiter.api.Assertions.assertNotNull(v2Etag);
        // a validator obtained from v2 is accepted by v1 for the same underlying document
        given().header("If-None-Match", v2Etag).get("/api/v1/documents/" + id).then().statusCode(304);
        given().get("/api/v2/documents/" + id).then().body("version", not(notNullValue()));
    }

    @Test
    void anUnknownDocumentIs404InBothVersions() {
        given().get("/api/v1/documents/999999").then().statusCode(404);
        given().get("/api/v2/documents/999999").then().statusCode(404);
    }
}
