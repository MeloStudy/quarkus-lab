package lab.melostudy;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

/** Test helper: builds reservation request bodies as raw JSON text, so the tests do not depend on the code under study. */
final class Requests {

    private Requests() {
    }

    /** A valid body; pass the JSON text (with its quotes) of the parts a test wants to vary. */
    static String body(String checkIn, String roomType, String deposit, String payment, String extra) {
        return "{\"guest_name\":\"Ana\",\"check_in\":" + checkIn + ",\"check_out\":\"2026-12-04\",\"room_type\":" + roomType
                + (deposit == null ? "" : ",\"deposit\":" + deposit)
                + (payment == null ? "" : ",\"payment\":" + payment)
                + (extra == null ? "" : "," + extra) + "}";
    }

    static String valid() {
        return body("\"2026-12-01\"", "\"suite\"", null, null, null);
    }

    static ValidatableResponse post(String json) {
        return given().contentType(ContentType.JSON).body(json).when().post("/reservations").then();
    }
}
