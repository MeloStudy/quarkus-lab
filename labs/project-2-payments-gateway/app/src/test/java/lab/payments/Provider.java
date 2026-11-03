package lab.payments;

import static io.restassured.RestAssured.given;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;

import io.restassured.path.json.JsonPath;

/**
 * Drives the fake provider over HTTP (it lives in another class loader than the tests) and reads what it saw. The
 * behaviors it can be scripted to have are listed in {@link ProviderStubServer}.
 */
final class Provider {

    private Provider() {
    }

    private static String control() {
        return ConfigProvider.getConfig().getValue("lab.test.provider-control", String.class);
    }

    private static void post(String path) {
        given().post(control() + "/__control/" + path).then().statusCode(204);
    }

    /** Healthy, instant, nothing recorded, reachable. */
    static void reset() {
        post("reset");
    }

    /** The next {@code count} calls of that operation ({@code charge}, {@code refund}, {@code lookup} or {@code all}) fail without being processed. */
    static void failNext(int count, int status, String operation) {
        post("fail?count=" + count + "&status=" + status + "&on=" + operation);
    }

    static void failNextCharges(int count, int status) {
        failNext(count, status, "charge");
    }

    /** Every call waits this long before the provider looks at it. */
    static void delay(int millis) {
        post("delay?ms=" + millis);
    }

    /** How long a {@code tok_slowcapture} charge waits, after charging, before answering. */
    static void slowCapture(int millis) {
        post("slow?ms=" + millis);
    }

    /** Closes the provider's port: connections are refused until {@link #up}. */
    static void down() {
        post("down");
    }

    /** The provider's port closes while it is handling its Nth call: the Nth call is processed, later ones find nobody. */
    static void crashAfter(int calls) {
        post("crash-after?calls=" + calls);
    }

    /** The next {@code count} refunds are made, and the provider closes the connection without answering. */
    static void loseRefundAnswers(int count) {
        post("lose-refund-answers?count=" + count);
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

    /** Charges the provider really made (the same key twice counts once). */
    static int charges() {
        return stats().getInt("charges");
    }

    static int refunds() {
        return stats().getInt("refunds");
    }

    /** One map per call received: {@code call}, {@code key}, {@code authorization}, {@code requestId}, {@code traceparent}. */
    static List<Map<String, Object>> seen() {
        return stats().getList("seen");
    }
}
