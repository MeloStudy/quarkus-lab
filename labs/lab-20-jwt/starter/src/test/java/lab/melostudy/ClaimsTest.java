package lab.melostudy;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 2: the service reads who the caller is from the verified token. */
@QuarkusTest
class ClaimsTest {

    @Test
    void standardClaimsAreExposed() {
        Api.as("alice@lab.example", "reader", "editor").get("/api/me").then()
                .statusCode(200)
                .body("name", equalTo("alice@lab.example"))
                .body("subject", equalTo("sub-alice@lab.example"))
                .body("issuer", equalTo(Tokens.ISSUER))
                .body("groups", containsInAnyOrder("reader", "editor"));
    }

    @Test
    void customClaimIsInjected() {
        String token = Tokens.valid("alice@lab.example", "reader").claim("email", "alice@mail.example").sign();
        Api.bearer(token).get("/api/me").then()
                .statusCode(200)
                .body("email", equalTo("alice@mail.example"));
    }

    @Test
    void missingCustomClaimIsNull() {
        Api.as("alice@lab.example", "reader").get("/api/me").then()
                .statusCode(200)
                .body("email", nullValue());
    }

    @Test
    void expirationComesFromTheToken() {
        long exp = Instant.now().plusSeconds(1800).getEpochSecond();
        String token = Tokens.valid("alice@lab.example", "reader").expiresAt(exp).sign();
        Api.bearer(token).get("/api/me").then()
                .statusCode(200)
                .body("expiresAt", equalTo((int) exp));
    }

    @Test
    void notesRecordTheAuthorFromTheToken() {
        Api.as("alice@lab.example", "editor").contentType("application/json").body("{\"title\":\"first\"}")
                .post("/api/notes").then().statusCode(201)
                .body("author", equalTo("alice@lab.example"));
    }
}
