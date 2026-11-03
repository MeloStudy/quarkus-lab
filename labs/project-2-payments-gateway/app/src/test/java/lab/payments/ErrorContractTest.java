package lab.payments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

/** Every failure has one shape: {@code {code, message, errors?, paymentId?, requestId}}. The code is for programs, the message for people, the requestId for support. */
@QuarkusTest
@TestProfile(LogProfile.class)
class ErrorContractTest extends PaymentsTestBase {


    private static Arguments failure(String name, int status, String code, Supplier<Response> trigger) {
        return Arguments.of(name, status, code, trigger);
    }

    private static String ana() {
        return Tokens.customer(Api.ANA);
    }

    private static String finance() {
        return Tokens.finance("emp_1");
    }

    static Stream<Arguments> failures() {
        return Stream.of(
                failure("invalid body", 400, "VALIDATION_ERROR", () -> Api.pay(ana(), "e-1", "{\"amount\":0}")),
                failure("broken json", 400, "MALFORMED_REQUEST", () -> Api.pay(ana(), "e-2", "{\"amount\":")),
                failure("no idempotency key", 400, "IDEMPOTENCY_KEY_REQUIRED", () -> Api.pay(ana(), null, Api.body(Api.CARD))),
                failure("unsafe idempotency key", 400, "INVALID_IDEMPOTENCY_KEY", () -> Api.pay(ana(), "has space", Api.body(Api.CARD))),
                failure("unknown payment", 404, "NOT_FOUND", () -> Api.get(ana(), "pay_doesnotexist")),
                failure("unknown route", 404, "NOT_FOUND", () -> Api.as(ana()).get("/api/nothing-here")),
                failure("wrong method", 405, "HTTP_405", () -> Api.as(finance()).delete("/api/payments/pay_x")),
                failure("not json", 415, "HTTP_415", () -> Api.as(ana()).contentType("text/plain").header("Idempotency-Key", "e-3").body("x").post("/api/payments")),
                failure("key reused", 422, "IDEMPOTENCY_KEY_REUSED", () -> {
                    Api.pay(ana(), "e-4", Api.body(2500, "EUR", Api.CARD)).then().statusCode(201);
                    return Api.pay(ana(), "e-4", Api.body(9900, "EUR", Api.CARD));
                }),
                failure("declined", 402, "CARD_DECLINED", () -> Api.pay(ana(), "e-5", Api.body("tok_declined1"))),
                failure("refund of a declined payment", 409, "INVALID_STATE", () -> {
                    String id = Api.pay(ana(), "e-6", Api.body("tok_declined1")).path("paymentId");
                    return Api.as(finance()).post("/api/payments/" + id + "/refund");
                }),
                failure("provider rejected", 502, "PROVIDER_REJECTED", () -> Api.pay(ana(), "e-7", Api.body("tok_rejected1"))),
                failure("provider down", 503, "PROVIDER_UNAVAILABLE", () -> {
                    Provider.down();
                    return Api.pay(ana(), "e-8", Api.body(Api.CARD));
                }),
                failure("unplanned failure", 500, "INTERNAL_ERROR", () -> Api.as(ana()).get("/api/_probe/boom")));
    }

    @ParameterizedTest(name = "{0} is {1} {2}")
    @MethodSource("failures")
    void everyFailureHasTheSameShape(String name, int status, String code, Supplier<Response> trigger) {
        Api.pay(ana(), "e-ok", Api.body(Api.CARD)).then().statusCode(201); // the service works before we break it
        Provider.reset();

        Response response = trigger.get();

        response.then().statusCode(status).contentType("application/json")
                .body("code", is(code)).body("message", is(notNullValue()))
                .body("requestId", is(response.header("X-Request-Id")));
    }

    @ParameterizedTest(name = "{0} reveals nothing internal")
    @MethodSource("failures")
    void noFailureRevealsInternals(String name, int status, String code, Supplier<Response> trigger) {
        Response response = trigger.get();

        String body = response.asString();
        assertThat(body, not(containsString("Exception")));
        assertThat(body, not(containsString("lab.payments")));
        assertThat(body, not(containsString("jdbc")));
        assertThat(body, not(containsString("\tat ")));
        assertThat(body, not(containsString(ProviderStub.API_KEY)));
    }

    @Test
    void aValidationErrorListsFieldsNotJavaPaths() {
        Response response = Api.pay(ana(), "e-9", "{\"amount\":0,\"currency\":\"GBP\",\"cardToken\":\"nope\"}");

        response.then().statusCode(400).body("errors.field", org.hamcrest.Matchers.containsInAnyOrder("amount", "currency", "cardToken"));
        assertThat(response.asString(), not(containsString("create.")));
        assertThat(response.asString(), not(containsString("arg0")));
    }

    @Test
    void emptyPartsAreOmittedNotSentAsNull() {
        Api.get(ana(), "pay_doesnotexist").then().statusCode(404).body("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasKey("errors")))
                .body("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasKey("paymentId")));
    }

    @Test
    void theTemporaryFailuresSayWhenToComeBack() {
        Provider.down();

        Api.pay(ana(), "e-10", Api.body(Api.CARD)).then().statusCode(503).header("Retry-After", is("10"));
    }

    @Test
    void aDeclineNamesThePaymentThatRecordsIt() {
        Response response = Api.pay(ana(), "e-11", Api.body("tok_declined1"));

        response.then().statusCode(402).body("paymentId", startsWith("pay_"));
    }
}
