package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Task 3: the {@code groups} claim of the token becomes the roles checked by {@code @RolesAllowed}. */
@QuarkusTest
class RolesTest {

    private static final String NEW_NOTE = "{\"title\":\"a note\"}";

    @Inject
    NoteStore store;

    @BeforeEach
    void emptyStore() {
        store.clear();
    }

    @Test
    void readerReadsButCannotWrite() {
        Api.as("rita", "reader").get("/api/notes").then().statusCode(200).body("$", hasSize(0));
        Api.as("rita", "reader").contentType("application/json").body(NEW_NOTE).post("/api/notes").then().statusCode(403);
    }

    @Test
    void editorWrites() {
        Api.as("eve", "editor").contentType("application/json").body(NEW_NOTE).post("/api/notes").then().statusCode(201);
        Api.as("eve", "editor").get("/api/notes").then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    void onlyAdminDeletes() {
        long id = store.add("eve", "to delete").id();
        Api.as("eve", "editor").delete("/api/notes/" + id).then().statusCode(403);
        Api.as("adam", "admin").delete("/api/notes/" + id).then().statusCode(204);
        Api.as("adam", "admin").delete("/api/notes/" + id).then().statusCode(404);
    }

    @Test
    void validTokenWithoutGroupsIsForbiddenNotUnauthorized() {
        Api.as("rita", "reader").get("/api/notes").then().statusCode(200);
        String token = Tokens.valid("nobody").sign();
        Api.bearer(token).get("/api/notes").then().statusCode(403);
    }

    @Test
    void noTokenIsUnauthorizedNotForbidden() {
        given().get("/api/notes").then().statusCode(401);
    }

    @Test
    void roleNamesAreCaseSensitive() {
        Api.as("rita", "reader").get("/api/notes").then().statusCode(200);
        Api.as("rita", "Reader").get("/api/notes").then().statusCode(403);
    }

    @Test
    void forgedAdminTokenDoesNotWork() {
        long id = store.add("eve", "keep me").id();
        Api.as("adam", "admin").get("/api/notes").then().statusCode(200);
        Api.bearer(Tokens.forged("adam", "admin")).delete("/api/notes/" + id).then().statusCode(401);
    }
}
