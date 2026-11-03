package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Task 4: {@code /api/summary} calls {@code /api/notes} over HTTP. The second call must carry the token of the
 * original caller, otherwise the downstream service sees nobody.
 */
@QuarkusTest
class PropagationTest {

    @Inject
    NoteStore store;

    @BeforeEach
    void oneNote() {
        store.clear();
        store.add("eve", "first note");
    }

    @Test
    void summaryIsBuiltWithTheCallersToken() {
        Api.as("rita").get("/api/summary").then().statusCode(200)
                .body("count", equalTo(1))
                .body("titles", contains("first note"));
    }

    @Test
    void summaryWithoutTokenIsUnauthorized() {
        Api.as("rita").get("/api/summary").then().statusCode(200);
        given().get("/api/summary").then().statusCode(401);
    }

    @Test
    void tokenForAnotherClientIsRefusedBeforeAnyCall() {
        Api.as("rita").get("/api/summary").then().statusCode(200);
        Api.bearer(Tokens.forBilling("rita")).get("/api/summary").then().statusCode(401);
    }
}
