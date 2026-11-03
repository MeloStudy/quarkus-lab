package lab.payments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

/**
 * What happens to the customer's money when the provider misbehaves (lab 23, applied). The question behind every test:
 * can the money have moved? Retrying is safe only because every attempt carries the same Idempotency-Key.
 */
@QuarkusTest
@TestProfile(LogProfile.class)
class ResilienceTest extends PaymentsTestBase {

    private final String ana = Tokens.customer(Api.ANA);
    private final String finance = Tokens.finance("emp_1");

    private String status(String id) {
        return Api.get(ana, id).then().statusCode(200).extract().path("status");
    }

    // ---- failures that a retry fixes

    @Test
    void aTransientFailureIsRetriedAndTheCardIsChargedOnce() {
        Provider.failNextCharges(1, 503);

        Response response = Api.pay(ana, "r-1", Api.body(Api.CARD));

        response.then().statusCode(201).body("status", is("CAPTURED"));
        assertThat("two calls: the failed one and the retry", Provider.calls(), is(2));
        assertThat("one charge", Provider.charges(), is(1));
        List<Map<String, Object>> seen = Provider.seen();
        assertThat("both calls carried the same key, the payment's id", seen.get(0).get("key"), is(seen.get(1).get("key")));
    }

    @Test
    void twoFailuresInARowAreStillRetriedAndCharged() {
        Provider.failNextCharges(2, 429);

        Api.pay(ana, "r-2", Api.body(Api.CARD)).then().statusCode(201).body("status", is("CAPTURED"));

        assertThat(Provider.calls(), is(3));
        assertThat(Provider.charges(), is(1));
    }

    @Test
    void aFirstAttemptThatTimesOutIsRetriedAndStillChargesOnce() {
        // The provider charges, and answers too late. The retry carries the same key: the provider answers at once with the same result.
        Response response = Api.pay(ana, "r-3", Api.body("tok_slowfirst1"));

        response.then().statusCode(201).body("status", is("CAPTURED"));
        assertThat(Provider.calls(), is(2));
        assertThat("the money moved once, not twice", Provider.charges(), is(1));
    }

    // ---- failures that leave us not knowing

    @Test
    void whenTheRetriesRunOutTheAnswerIs202UnknownNotAFailure() {
        Provider.failNextCharges(10, 503);

        Response response = Api.pay(ana, "r-4", Api.body(Api.CARD));

        response.then().statusCode(202).body("status", is("UNKNOWN"));
        String location = response.header("Location");
        assertThat(location, matchesPattern(".*/api/payments/pay_[0-9a-f]{20}"));
        assertThat("one try and two retries", Provider.calls(), is(3));
        Api.as(ana).get(location).then().statusCode(200).body("status", is("UNKNOWN"));
    }

    @Test
    void aProviderThatAlwaysAnswers500LeavesTheStatusUnknown() {
        Response response = Api.pay(ana, "r-5", Api.body("tok_broken123"));

        response.then().statusCode(202).body("status", is("UNKNOWN"));
        assertThat(Provider.calls(), is(3));
    }

    @Test
    void aTimeoutAfterTheProviderChargedIsUnknownAndTheMoneyDidMove() {
        Response response = Api.pay(ana, "r-6", Api.body("tok_slowcapture1"));

        response.then().statusCode(202).body("status", is("UNKNOWN"));
        assertThat("the provider charged the card although we were told nothing", Provider.charges(), is(1));
    }

    @Test
    void aDroppedConnectionIsUnknown() {
        Api.pay(ana, "r-7", Api.body("tok_dropconn1")).then().statusCode(202).body("status", is("UNKNOWN"));

        assertThat(Provider.charges(), is(1));
    }

    @Test
    void anAnswerThatIsNotJsonIsUnknown() {
        Api.pay(ana, "r-8", Api.body("tok_garbage1")).then().statusCode(202).body("status", is("UNKNOWN"));

        assertThat(Provider.charges(), is(1));
    }

    @Test
    void askingAgainWithTheSameKeyAfterAnUnknownIsAReplayNotANewCharge() {
        Provider.failNextCharges(10, 503);
        Response first = Api.pay(ana, "r-9", Api.body(Api.CARD));
        first.then().statusCode(202);
        int callsBefore = Provider.calls();
        Provider.reset();

        Response again = Api.pay(ana, "r-9", Api.body(Api.CARD));

        again.then().statusCode(202).header("Idempotent-Replayed", is("true")).body("id", is((Object) first.path("id")));
        assertThat("the provider was not asked again", Provider.calls(), is(0));
        assertThat(callsBefore, is(3));
    }

    // ---- failures that prove nothing was sent

    @Test
    void whenTheProviderCannotBeReachedNothingIsRecordedAndTheKeyIsFree() {
        Provider.down();

        Response response = Api.pay(ana, "r-10", Api.body(Api.CARD));

        response.then().statusCode(503).body("code", is("PROVIDER_UNAVAILABLE")).header("Retry-After", is("10"));
        Api.as(ana).get("/api/payments").then().statusCode(200).body("$", is(empty()));

        // The same key, once the provider is back, is a first request again.
        Provider.up();
        Api.pay(ana, "r-10", Api.body(Api.CARD)).then().statusCode(201).body("status", is("CAPTURED"))
                .header("Idempotent-Replayed", is(nullValue()));
        assertThat(Provider.charges(), is(1));
    }

    @Test
    void aRefusedConnectionAfterAnAttemptThatMayHaveChargedDoesNotMakeItSafe() {
        // The first attempt reaches the provider, which charges and answers too late. Then the provider goes away: the retries
        // are refused at the door. "Refused" says nothing about what the first attempt did, so the payment stays UNKNOWN.
        Provider.crashAfter(1);

        Response response = Api.pay(ana, "r-12", Api.body("tok_slowcapture1"));

        response.then().statusCode(202).body("status", is("UNKNOWN"));
        assertThat("the first attempt really charged", Provider.charges(), is(1));
        assertThat("only the first attempt arrived", Provider.calls(), is(1));
    }

    // ---- the answer to "did it really fail?"

    @Test
    void anUnknownPaymentThatTheProviderNeverSawBecomesFailedWhenReconciled() {
        Provider.failNextCharges(10, 503);
        String id = Api.pay(ana, "r-13", Api.body(Api.CARD)).then().statusCode(202).extract().path("id");
        Provider.reset();

        Api.as(finance).post("/api/payments/" + id + "/reconcile").then().statusCode(200)
                .body("status", is("FAILED")).body("declineReason", is("never_reached_provider"));
        assertThat("nobody was charged", Provider.charges(), is(0));
    }

    @Test
    void anUnknownPaymentThatTheProviderChargedBecomesCapturedWithoutChargingAgain() {
        String id = Api.pay(ana, "r-14", Api.body("tok_slowcapture1")).then().statusCode(202).extract().path("id");

        Api.as(finance).post("/api/payments/" + id + "/reconcile").then().statusCode(200)
                .body("status", is("CAPTURED")).body("providerReference", matchesPattern("ch_\\d+"));
        assertThat("reconciling asks, it does not charge", Provider.charges(), is(1));
        assertThat(status(id), is("CAPTURED"));
        // The customer who retries with the key they used learns the truth, as a replay.
        Api.pay(ana, "r-14", Api.body("tok_slowcapture1")).then().statusCode(201).header("Idempotent-Replayed", is("true"))
                .body("status", is("CAPTURED"));
    }

    @Test
    void theUnknownStatusIsNeverTurnedIntoAnErrorTheCustomerCouldActOn() {
        Provider.failNextCharges(10, 503);

        Response response = Api.pay(ana, "r-15", Api.body(Api.CARD));

        // Not a 5xx: a client that sees a 5xx will pay again with a new key. A 202 says "accepted, outcome pending".
        response.then().statusCode(202);
        assertThat(response.asString(), not(containsString("error")));
    }
}
