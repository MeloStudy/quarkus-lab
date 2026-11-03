package lab.payments;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Duration;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

/** Who may call what (labs 19 and 20): every rule has an allowed and a refused request, and the refusals first prove the allowed one works. */
@QuarkusTest
@TestProfile(LogProfile.class)
class AuthenticationTest extends PaymentsTestBase {

    private static String paymentIdOfAna() {
        return Api.pay("auth-setup", Api.CARD).then().statusCode(201).extract().path("id");
    }

    // ---- authentication: is this a token we issued, for this API?

    @Test
    void aValidCustomerTokenIsAccepted() {
        Api.pay("a-1", Api.CARD).then().statusCode(201);
    }

    @Test
    void noTokenIs401() {
        Api.anonymous().header("Idempotency-Key", "a-2").body(Api.body(Api.CARD)).post("/api/payments").then().statusCode(401);
    }

    @Test
    void aTokenSignedWithAnotherKeyIs401() {
        Api.pay(Tokens.forged(Api.ANA, "customer"), "a-3", Api.body(Api.CARD)).then().statusCode(401);
    }

    /** Suppliers, not tokens: signing needs the configuration of the running application. */
    static Stream<Arguments> badTokens() {
        return Stream.of(
                token("wrong issuer", () -> Tokens.valid(Api.ANA, "customer").issuer("https://evil.example/issuer").sign()),
                token("wrong audience", () -> Tokens.valid(Api.ANA, "customer").audience("another-api").sign()),
                token("expired", () -> Tokens.valid(Api.ANA, "customer").expiresIn(Duration.ofSeconds(-120)).sign()),
                token("not a token at all", () -> "this.is.not-a-token"));
    }

    private static Arguments token(String why, Supplier<String> token) {
        return Arguments.of(why, token);
    }

    @ParameterizedTest(name = "{0} is 401")
    @MethodSource("badTokens")
    void tokensThatAreNotForThisApiAre401(String why, Supplier<String> token) {
        Api.pay(Tokens.customer(Api.ANA), "a-4-ok", Api.body(Api.CARD)).then().statusCode(201); // the same request with a good token works
        Api.pay(token.get(), "a-4", Api.body(Api.CARD)).then().statusCode(401);
    }

    // ---- authorization: what may this role do?

    @Test
    void financeCannotCreatePayments() {
        Api.pay(Tokens.finance("emp_1"), "a-5", Api.body(Api.CARD)).then().statusCode(403);
    }

    @Test
    void aCustomerCannotRefundOrReconcile() {
        String id = paymentIdOfAna();
        Api.as(Tokens.finance("emp_1")).post("/api/payments/" + id + "/refund").then().statusCode(200);
        Api.as(Tokens.customer(Api.ANA)).post("/api/payments/" + id + "/refund").then().statusCode(403);
        Api.as(Tokens.customer(Api.ANA)).post("/api/payments/" + id + "/reconcile").then().statusCode(403);
    }

    @Test
    void aTokenWithoutAKnownRoleCanDoNothing() {
        String stranger = Tokens.valid("cus_zoe", "visitor").sign();
        Api.pay(Tokens.customer(Api.ANA), "a-6-ok", Api.body(Api.CARD)).then().statusCode(201);
        Api.pay(stranger, "a-6", Api.body(Api.CARD)).then().statusCode(403);
        Api.as(stranger).get("/api/payments").then().statusCode(403);
    }

    @Test
    void securityIsCheckedBeforeTheBodyIsLookedAt() {
        // An invalid body from somebody who may not call is a 401 / 403, not a 400 that teaches them the contract.
        Api.anonymous().body("{\"amount\":0}").post("/api/payments").then().statusCode(401);
        Api.as(Tokens.finance("emp_1")).body("{\"amount\":0}").post("/api/payments").then().statusCode(403);
        Api.as(Tokens.customer(Api.ANA)).header("Idempotency-Key", "a-7").body("{\"amount\":0}").post("/api/payments").then().statusCode(400);
    }

    // ---- the default is closed

    @Test
    void anEndpointNobodyAnnotatedIsClosedEvenForAnAuthenticatedUser() {
        Api.as(Tokens.customer(Api.ANA)).get("/api/_probe/boom").then().statusCode(500); // the probe next to it is reachable
        Api.as(Tokens.customer(Api.ANA)).get("/api/_probe/closed").then().statusCode(403);
        Api.anonymous().get("/api/_probe/closed").then().statusCode(401);
    }

    // ---- support needs an id on every answer, the refused ones included

    static Stream<Arguments> everyKindOfAnswer() {
        return Stream.of(
                call("401 without a token", () -> Api.anonymous().body(Api.body(Api.CARD)).post("/api/payments")),
                call("401 forged token", () -> Api.pay(Tokens.forged(Api.ANA, "customer"), "r-1", Api.body(Api.CARD))),
                call("403 wrong role", () -> Api.pay(Tokens.finance("emp_1"), "r-2", Api.body(Api.CARD))),
                call("404 unknown route", () -> Api.as(Tokens.customer(Api.ANA)).get("/api/nothing-here")),
                call("400 invalid body", () -> Api.pay(Tokens.customer(Api.ANA), "r-3", "{\"amount\":0}")),
                call("201 created", () -> Api.pay("r-4", Api.CARD)));
    }

    private static Arguments call(String kind, Supplier<Response> call) {
        return Arguments.of(kind, call);
    }

    @ParameterizedTest(name = "{0} carries X-Request-Id")
    @MethodSource("everyKindOfAnswer")
    void everyAnswerCarriesTheRequestId(String kind, Supplier<Response> call) {
        call.get().then().header("X-Request-Id", is(notNullValue()));
    }
}
