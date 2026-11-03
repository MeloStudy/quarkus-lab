package lab.tracker;

import static io.restassured.RestAssured.given;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Test-only HTTP shortcuts. Everything goes through the real API (raw JSON strings, no production classes), so the
 * acceptance tests read as a client would write them and keep working if the internals are rewritten.
 */
final class Tracker {

    private static final List<String> PATH = List.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");

    private Tracker() {
    }

    static Response project(String code) {
        return given().contentType(ContentType.JSON).body("{\"code\":\"" + code + "\",\"name\":\"Project " + code + "\"}")
                .post("/api/projects");
    }

    static Response open(String project, String body) {
        return given().contentType(ContentType.JSON).body(body).post("/api/projects/{code}/issues", project);
    }

    /** Creates a BUG with MEDIUM priority and returns its key; asserts that it worked. */
    static String issue(String project, String title) {
        Response r = open(project, "{\"title\":\"" + title + "\",\"type\":\"BUG\"}");
        r.then().statusCode(201);
        return r.jsonPath().getString("key");
    }

    static Response get(String key) {
        return given().get("/api/issues/{key}", key);
    }

    static String etag(String key) {
        return get(key).header("ETag");
    }

    static Response put(String key, String ifMatch, String body) {
        RequestSpecification spec = given().contentType(ContentType.JSON);
        if (ifMatch != null) {
            spec = spec.header("If-Match", ifMatch);
        }
        return spec.body(body).put("/api/issues/{key}", key);
    }

    /** A valid PUT body; a null assignee is simply left out. */
    static String updateBody(String title, String priority, String assignee) {
        return "{\"title\":\"" + title + "\",\"priority\":\"" + priority + "\""
                + (assignee == null ? "" : ",\"assignee\":\"" + assignee + "\"") + "}";
    }

    static Response transition(String key, String ifMatch, String to) {
        RequestSpecification spec = given().contentType(ContentType.JSON);
        if (ifMatch != null) {
            spec = spec.header("If-Match", ifMatch);
        }
        return spec.body("{\"to\":\"" + to + "\"}").post("/api/issues/{key}/transitions", key);
    }

    /**
     * Sets the assignee and keeps everything else (PUT replaces the whole editable part, so the current description
     * and labels must travel with it), using the current ETag.
     */
    static void assign(String key, String assignee) {
        Response current = get(key);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", current.jsonPath().getString("title"));
        body.put("priority", current.jsonPath().getString("priority"));
        body.put("description", current.jsonPath().getString("description"));
        body.put("labels", current.jsonPath().getList("labels"));
        body.put("assignee", assignee);
        try {
            put(key, current.header("ETag"), new ObjectMapper().writeValueAsString(body)).then().statusCode(200);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** One transition with the current ETag. */
    static Response move(String key, String to) {
        return transition(key, etag(key), to);
    }

    /** Walks OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED up to {@code target}, assigning "alice" on the way. */
    static void advanceTo(String key, String target) {
        assign(key, "alice");
        for (int i = 1; i <= PATH.indexOf(target); i++) {
            move(key, PATH.get(i)).then().statusCode(200);
        }
    }

    static long count(Response list) {
        return Long.parseLong(list.header("X-Total-Count"));
    }
}
