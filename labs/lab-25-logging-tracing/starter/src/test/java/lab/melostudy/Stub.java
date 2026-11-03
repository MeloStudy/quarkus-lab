package lab.melostudy;

import static io.restassured.RestAssured.given;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;

/** What the fake stock service saw. */
final class Stub {

    private Stub() {
    }

    private static String base() {
        return ConfigProvider.getConfig().getValue("lab.test.stub-url", String.class);
    }

    static void reset() {
        given().post(base() + "/__control/reset").then().statusCode(204);
    }

    /** One map per call received, with the keys {@code requestId} and {@code traceparent} (null when absent). */
    static List<Map<String, Object>> requests() {
        return given().get(base() + "/__control/requests").jsonPath().getList("$");
    }
}
