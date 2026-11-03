package lab.payments;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** The requests the tests send, as a client would. Raw JSON strings: the tests never import the production DTOs. */
final class Api {

    static final String ANA = "cus_ana";
    static final String BEN = "cus_ben";
    static final String CARD = "tok_visaok1234";

    private Api() {
    }

    static String body(long amount, String currency, String token) {
        return "{\"amount\":" + amount + ",\"currency\":\"" + currency + "\",\"cardToken\":\"" + token + "\"}";
    }

    static String body(String token) {
        return body(2500, "EUR", token);
    }

    static RequestSpecification as(String token) {
        return given().auth().oauth2(token).contentType(ContentType.JSON);
    }

    static RequestSpecification anonymous() {
        return given().contentType(ContentType.JSON);
    }

    /** A payment of 25.00 EUR made by Ana with the given idempotency key and card token. */
    static Response pay(String idempotencyKey, String cardToken) {
        return pay(Tokens.customer(ANA), idempotencyKey, body(cardToken));
    }

    static Response pay(String bearer, String idempotencyKey, String body) {
        RequestSpecification request = as(bearer);
        if (idempotencyKey != null) {
            request = request.header("Idempotency-Key", idempotencyKey);
        }
        return request.body(body).post("/api/payments");
    }

    static Response get(String bearer, String id) {
        return as(bearer).get("/api/payments/" + id);
    }
}
