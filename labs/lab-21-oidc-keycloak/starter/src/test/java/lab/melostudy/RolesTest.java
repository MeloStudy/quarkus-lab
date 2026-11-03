package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Task 2: the realm roles in the token become the roles checked by {@code @RolesAllowed}. */
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
    void identityHasTheRolesOfTheRealm() {
        Api.as("eve").get("/api/me").then().statusCode(200).body("roles", containsInAnyOrder("reader", "editor"));
    }

    @Test
    void readerReadsButCannotWrite() {
        Api.as("rita").get("/api/notes").then().statusCode(200).body("$", hasSize(0));
        Api.as("rita").contentType("application/json").body(NEW_NOTE).post("/api/notes").then().statusCode(403);
    }

    @Test
    void editorWrites() {
        Api.as("eve").contentType("application/json").body(NEW_NOTE).post("/api/notes").then().statusCode(201);
        Api.as("eve").get("/api/notes").then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    void onlyAdminDeletes() {
        long id = store.add("eve", "to delete").id();
        Api.as("eve").delete("/api/notes/" + id).then().statusCode(403);
        Api.as("adam").delete("/api/notes/" + id).then().statusCode(204);
        Api.as("adam").delete("/api/notes/" + id).then().statusCode(404);
    }

    @Test
    void noTokenIsUnauthorizedNotForbidden() {
        given().get("/api/notes").then().statusCode(401);
    }

    @Test
    void authorIsTheNameOfTheUser() {
        Api.as("eve").contentType("application/json").body(NEW_NOTE).post("/api/notes").then().statusCode(201)
                .body("author", equalTo("eve"));
    }
}
