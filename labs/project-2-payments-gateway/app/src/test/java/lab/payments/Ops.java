package lab.payments;

import static io.restassured.RestAssured.given;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.restassured.response.Response;

/** Calls to the operations side of the application: health and metrics, on the management port of the tests. */
public final class Ops {

    public static final int MANAGEMENT_PORT = 9001;

    private Ops() {
    }

    public static Response get(String path) {
        return given().port(MANAGEMENT_PORT).get(path);
    }

    public static Response onPublicPort(String path) {
        return given().get(path);
    }

    /** The whole Prometheus text. */
    public static String metrics() {
        return get("/q/metrics").then().statusCode(200).extract().asString();
    }

    /**
     * The value of the first sample whose line starts with {@code name} followed by a label set that matches
     * {@code labelsRegex} (or that has no labels at all), or {@code 0} when no such sample exists yet.
     */
    public static double sample(String metrics, String name, String labelsRegex) {
        // A sample may end with an exemplar ("# {trace_id=...} 1.0 1793..."): what follows the value is not part of it.
        Pattern line = Pattern.compile("^" + Pattern.quote(name) + "(?:\\{(" + labelsRegex + ")\\})?\\s+(\\S+)(?:\\s+#.*)?$", Pattern.MULTILINE);
        Matcher matcher = line.matcher(metrics);
        return matcher.find() ? Double.parseDouble(matcher.group(2)) : 0;
    }
}
