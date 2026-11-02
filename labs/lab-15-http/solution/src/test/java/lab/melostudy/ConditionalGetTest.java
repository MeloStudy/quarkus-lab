package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

/** Task 1: a representation carries a validator (ETag) and a client can ask "only if it changed". */
@QuarkusTest
class ConditionalGetTest {

    @Inject
    TestSupport support;

    @BeforeEach
    void reset() {
        support.reset();
    }

    @Test
    void createAndGetCarryAnEtag() {
        Response created = TestSupport.createDocument("Plan", "v1");
        created.then().statusCode(201).header("ETag", not(emptyOrNullString()));
        String url = TestSupport.urlOf(created);
        given().get(url).then().statusCode(200).header("ETag", equalTo(created.getHeader("ETag")));
    }

    @Test
    void ifNoneMatchWithTheCurrentEtagAnswers304WithoutBody() {
        Response created = TestSupport.createDocument("Plan", "v1");
        String url = TestSupport.urlOf(created);
        String etag = given().get(url).getHeader("ETag");

        given().header("If-None-Match", etag).get(url)
                .then().statusCode(304).body(emptyOrNullString()).header("ETag", equalTo(etag));
    }

    @Test
    void ifNoneMatchWithAnotherEtagAnswers200WithTheBody() {
        Response created = TestSupport.createDocument("Plan", "v1");
        String url = TestSupport.urlOf(created);

        given().header("If-None-Match", "\"999\"").get(url).then().statusCode(200).body("title", equalTo("Plan"));
    }

    @Test
    void theEtagChangesWhenTheDocumentChanges() {
        Response created = TestSupport.createDocument("Plan", "v1");
        String url = TestSupport.urlOf(created);
        String before = given().get(url).getHeader("ETag");

        given().contentType(ContentType.JSON).header("If-Match", before)
                .body("{\"title\":\"Plan\",\"content\":\"v2\"}").put(url).then().statusCode(200);

        // the old validator no longer matches: the client gets the new representation, not a 304
        given().header("If-None-Match", before).get(url).then().statusCode(200).body("content", equalTo("v2"));
        String after = given().get(url).getHeader("ETag");
        org.junit.jupiter.api.Assertions.assertNotEquals(before, after);
    }

    @Test
    void anUnknownDocumentIs404EvenWithAValidator() {
        given().get("/api/v1/documents/999999").then().statusCode(404);
        given().header("If-None-Match", "*").get("/api/v1/documents/999999").then().statusCode(404);
    }
}
