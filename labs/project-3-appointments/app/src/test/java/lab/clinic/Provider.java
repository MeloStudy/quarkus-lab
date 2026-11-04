package lab.clinic;

import static io.restassured.RestAssured.given;

import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;

import io.restassured.path.json.JsonPath;

/**
 * Drives the fake notification provider over HTTP (it lives in another class loader than the tests) and reads what it saw.
 * The behaviors it can be scripted to have are listed in {@link NotificationStubServer}.
 */
final class Provider {

    private Provider() {
    }

    private static String control() {
        return ConfigProvider.getConfig().getValue("lab.test.notifications-control", String.class);
    }

    private static void post(String path) {
        given().post(control() + "/__control/" + path).then().statusCode(204);
    }

    /** Healthy, instant, nothing recorded, reachable. */
    static void reset() {
        post("reset");
    }

    /** The next {@code count} calls fail with {@code status} without being processed. */
    static void failNext(int count, int status) {
        post("fail?count=" + count + "&status=" + status);
    }

    /** Every call waits this long before the provider looks at it. */
    static void delay(int millis) {
        post("delay?ms=" + millis);
    }

    /** Closes the provider's port: connections are refused until {@link #up}. */
    static void down() {
        post("down");
    }

    static void up() {
        post("up");
    }

    private static JsonPath stats() {
        return given().get(control() + "/__control/stats").jsonPath();
    }

    /** HTTP calls the provider received, retries included. */
    static int calls() {
        return stats().getInt("calls");
    }

    /** Messages the provider really delivered (the same key twice counts once). */
    static int deliveries() {
        return stats().getInt("deliveries");
    }

    /** One entry per call received: {@code key -> recipient}. */
    static List<String> seen() {
        return stats().getList("seen");
    }

    /** The {@code traceparent} header of each call received ("" when it had none). */
    static List<String> traceparents() {
        return stats().getList("traceparents");
    }
}
