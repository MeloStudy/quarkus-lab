package lab.payments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

/** What a successful payment looks like, what the provider is sent, and what a request must satisfy to be accepted at all. */
@QuarkusTest
@TestProfile(LogProfile.class)
class PaymentCreationTest extends PaymentsTestBase {

    @Test
    void aCapturedPaymentIs201WithItsLocationAndOnlyWhatTheClientMayKnow() {
        Response response = Api.pay(Tokens.customer(Api.ANA), "c-1", "{\"amount\":2500,\"currency\":\"EUR\",\"cardToken\":\"" + Api.CARD
                + "\",\"description\":\"Order 17\"}");

        response.then().statusCode(201)
                .body("id", matchesPattern("pay_[0-9a-f]{20}"))
                .body("status", is("CAPTURED"))
                .body("amount", is(2500))
                .body("currency", is("EUR"))
                .body("description", is("Order 17"))
                .body("providerReference", matchesPattern("ch_\\d+"))
                .body("createdAt", matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z"))
                .body("declineReason", is(nullValue()));
        assertThat(response.header("Location"), endsWith("/api/payments/" + response.path("id")));
        // Nothing the client must not see: not the card, not the customer, not the idempotency key.
        assertThat(response.asString(), not(containsString(Api.CARD)));
        assertThat(response.asString(), not(containsString(Api.ANA)));
        assertThat(response.asString(), not(containsString("c-1\"")));
    }

    @Test
    void theProviderIsAskedOnceWithOurCredentialsTheRequestIdAndAKeyOfItsOwn() {
        Response response = Api.as(Tokens.customer(Api.ANA)).header("Idempotency-Key", "c-2").header("X-Request-Id", "req-c-2")
                .body(Api.body(Api.CARD)).post("/api/payments");

        response.then().statusCode(201).header("X-Request-Id", is("req-c-2"));
        List<Map<String, Object>> calls = Provider.seen();
        assertThat(calls.size(), is(1));
        Map<String, Object> call = calls.get(0);
        assertThat(call.get("call"), is("POST /charges"));
        assertThat(call.get("authorization"), is("Bearer " + ProviderStub.API_KEY));
        assertThat(call.get("requestId"), is("req-c-2"));
        // The provider's idempotency key is the payment's own id, not the customer's key: two customers may choose the same key.
        String paymentId = response.path("id");
        assertThat(call.get("key"), is(paymentId));
        assertThat(Provider.charges(), is(1));
    }

    // ---- what a request must satisfy

    private static Arguments invalid(String name, String body, String field) {
        return Arguments.of(name, body, field);
    }

    static Stream<Arguments> invalidBodies() {
        String ok = "\"currency\":\"EUR\",\"cardToken\":\"tok_visaok1234\"";
        return Stream.of(
                invalid("zero amount", "{\"amount\":0," + ok + "}", "amount"),
                invalid("negative amount", "{\"amount\":-5," + ok + "}", "amount"),
                invalid("amount above the limit", "{\"amount\":10000001," + ok + "}", "amount"),
                invalid("no amount", "{" + ok + "}", "amount"),
                invalid("unsupported currency", "{\"amount\":100,\"currency\":\"GBP\",\"cardToken\":\"tok_visaok1234\"}", "currency"),
                invalid("lower case currency", "{\"amount\":100,\"currency\":\"eur\",\"cardToken\":\"tok_visaok1234\"}", "currency"),
                invalid("no currency", "{\"amount\":100,\"cardToken\":\"tok_visaok1234\"}", "currency"),
                invalid("no card token", "{\"amount\":100,\"currency\":\"EUR\"}", "cardToken"),
                invalid("token too short", "{\"amount\":100,\"currency\":\"EUR\",\"cardToken\":\"tok_abc\"}", "cardToken"),
                invalid("token without the prefix", "{\"amount\":100,\"currency\":\"EUR\",\"cardToken\":\"visaok12345\"}", "cardToken"),
                invalid("a raw card number instead of a token", "{\"amount\":100,\"currency\":\"EUR\",\"cardToken\":\"4111111111111111\"}", "cardToken"),
                invalid("description too long", "{\"amount\":100,\"currency\":\"EUR\",\"cardToken\":\"tok_visaok1234\",\"description\":\"" + "d".repeat(141) + "\"}", "description"));
    }

    @ParameterizedTest(name = "{0} is 400 and names {2}")
    @MethodSource("invalidBodies")
    void aRequestThatBreaksTheContractIs400AndNamesTheField(String name, String body, String field) {
        Api.pay("c-ok-first", Api.CARD).then().statusCode(201); // a valid request works
        Provider.reset();

        Api.pay(Tokens.customer(Api.ANA), "c-3", body).then().statusCode(400)
                .body("code", is("VALIDATION_ERROR"))
                .body("errors.field", containsInAnyOrder(field));
        assertThat("nothing reached the provider", Provider.calls(), is(0));
    }

    @Test
    void aValidationErrorNeverRepeatsWhatWasSent() {
        String pan = "4111111111111111";
        Response response = Api.pay(Tokens.customer(Api.ANA), "c-4", "{\"amount\":100,\"currency\":\"EUR\",\"cardToken\":\"" + pan + "\"}");

        response.then().statusCode(400);
        assertThat(response.asString(), not(containsString(pan)));
    }

    @Test
    void aFieldTheContractDoesNotListIsAClientBugNotAnIgnoredField() {
        Api.pay("c-5-ok", Api.CARD).then().statusCode(201);
        String body = "{\"amount\":100,\"currency\":\"EUR\",\"cardToken\":\"tok_visaok1234\",\"status\":\"CAPTURED\"}";

        Api.pay(Tokens.customer(Api.ANA), "c-5", body).then().statusCode(400).body("code", is("MALFORMED_REQUEST"));
    }

    @Test
    void brokenJsonIs400() {
        Api.pay(Tokens.customer(Api.ANA), "c-6", "{\"amount\":").then().statusCode(400).body("code", is("MALFORMED_REQUEST"));
    }

    @Test
    void anAmountThatIsNotANumberIs400() {
        Api.pay(Tokens.customer(Api.ANA), "c-7", "{\"amount\":\"ten\",\"currency\":\"EUR\",\"cardToken\":\"tok_visaok1234\"}")
                .then().statusCode(400).body("code", is("MALFORMED_REQUEST"));
    }

    // ---- the idempotency key itself

    @Test
    void theKeyIsRequired() {
        Api.pay("c-8-ok", Api.CARD).then().statusCode(201);

        Api.pay(Tokens.customer(Api.ANA), null, Api.body(Api.CARD)).then().statusCode(400).body("code", is("IDEMPOTENCY_KEY_REQUIRED"));
        Api.pay(Tokens.customer(Api.ANA), "   ", Api.body(Api.CARD)).then().statusCode(400).body("code", is("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @ParameterizedTest(name = "key \"{0}\" is refused")
    @org.junit.jupiter.params.provider.ValueSource(strings = { "has space", "semi;colon", "<script>", "with/slash" })
    void aKeyThatIsNotSafeToStoreAndLogIsRefused(String key) {
        Api.pay("c-9-ok", Api.CARD).then().statusCode(201);

        Api.pay(Tokens.customer(Api.ANA), key, Api.body(Api.CARD)).then().statusCode(400).body("code", is("INVALID_IDEMPOTENCY_KEY"));
    }

    @Test
    void aKeyOfSixtyFiveCharactersIsRefusedAndOneOfSixtyFourIsAccepted() {
        Api.pay(Tokens.customer(Api.ANA), "k".repeat(65), Api.body(Api.CARD)).then().statusCode(400).body("code", is("INVALID_IDEMPOTENCY_KEY"));
        Api.pay(Tokens.customer(Api.ANA), "k".repeat(64), Api.body(Api.CARD)).then().statusCode(201);
    }

    @Test
    void anythingThatIsNotJsonIs415() {
        Api.as(Tokens.customer(Api.ANA)).contentType("text/plain").header("Idempotency-Key", "c-10").body("hello").post("/api/payments")
                .then().statusCode(415).body("code", anyOf(is("HTTP_415"), is("UNSUPPORTED_MEDIA_TYPE")));
    }
}
