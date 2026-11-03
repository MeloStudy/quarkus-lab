package lab.payments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/** The two things finance does by hand: settle a payment whose outcome we lost, and give money back. */
@QuarkusTest
@TestProfile(LogProfile.class)
class ReconcileAndRefundTest extends PaymentsTestBase {

    private final String ana = Tokens.customer(Api.ANA);
    private final String finance = Tokens.finance("emp_1");

    private String capture(String key) {
        return Api.pay(ana, key, Api.body(Api.CARD)).then().statusCode(201).extract().path("id");
    }

    /** The provider decided, but its answer never reached us. */
    private String unknownAfterTheProviderDecided(String key, String token) {
        return Api.pay(ana, key, Api.body(token)).then().statusCode(202).extract().path("id");
    }

    // ---- reconcile

    @Test
    void reconcileSettlesACapturedPaymentWeNeverHeardAbout() {
        String id = unknownAfterTheProviderDecided("rc-1", "tok_slowcapture1");

        Api.as(finance).post("/api/payments/" + id + "/reconcile").then().statusCode(200).body("status", is("CAPTURED"));
    }

    @Test
    void reconcileSettlesADeclinedPaymentWeNeverHeardAbout() {
        String id = unknownAfterTheProviderDecided("rc-2", "tok_declinedslow");

        Api.as(finance).post("/api/payments/" + id + "/reconcile").then().statusCode(200)
                .body("status", is("DECLINED")).body("declineReason", is("card_declined"));
        assertThat(Provider.charges(), is(0));
    }

    @Test
    void onlyUnknownPaymentsCanBeReconciled() {
        String captured = capture("rc-3");

        Api.as(finance).post("/api/payments/" + captured + "/reconcile").then().statusCode(409).body("code", is("INVALID_STATE"));
        Api.as(finance).post("/api/payments/pay_doesnotexist/reconcile").then().statusCode(404);
    }

    @Test
    void whenTheLookupFailsThePaymentStaysUnknown() {
        String id = unknownAfterTheProviderDecided("rc-4", "tok_slowcapture1");
        Provider.down();

        Api.as(finance).post("/api/payments/" + id + "/reconcile").then().statusCode(503).body("code", is("PROVIDER_UNAVAILABLE"));
        Provider.up();
        Api.get(ana, id).then().body("status", is("UNKNOWN"));
        Api.as(finance).post("/api/payments/" + id + "/reconcile").then().statusCode(200).body("status", is("CAPTURED"));
    }

    @Test
    void aFlakyLookupIsRetried() {
        String id = unknownAfterTheProviderDecided("rc-5", "tok_slowcapture1");
        Provider.failNext(1, 503, "lookup");

        Api.as(finance).post("/api/payments/" + id + "/reconcile").then().statusCode(200).body("status", is("CAPTURED"));
    }

    // ---- refund

    @Test
    void financeRefundsACapturedPaymentOnce() {
        String id = capture("rf-1");

        Api.as(finance).post("/api/payments/" + id + "/refund").then().statusCode(200).body("status", is("REFUNDED"));
        assertThat(Provider.refunds(), is(1));
    }

    @Test
    void refundingTwiceIsHarmlessAndRefundsOnce() {
        String id = capture("rf-2");

        Api.as(finance).post("/api/payments/" + id + "/refund").then().statusCode(200);
        Api.as(finance).post("/api/payments/" + id + "/refund").then().statusCode(200).body("status", is("REFUNDED"));
        assertThat("the money went back once", Provider.refunds(), is(1));
    }

    @Test
    void onlyACapturedPaymentCanBeRefunded() {
        String declined = Api.pay(ana, "rf-3", Api.body("tok_declined1")).then().statusCode(402).extract().path("paymentId");
        String unknown = unknownAfterTheProviderDecided("rf-3b", "tok_slowcapture1");

        Api.as(finance).post("/api/payments/" + declined + "/refund").then().statusCode(409).body("code", is("INVALID_STATE"));
        Api.as(finance).post("/api/payments/" + unknown + "/refund").then().statusCode(409).body("code", is("INVALID_STATE"));
        Api.as(finance).post("/api/payments/pay_doesnotexist/refund").then().statusCode(404);
        assertThat(Provider.refunds(), is(0));
    }

    @Test
    void aFlakyRefundIsRetriedAndStillRefundsOnce() {
        String id = capture("rf-4");
        Provider.failNext(1, 503, "refund");

        Api.as(finance).post("/api/payments/" + id + "/refund").then().statusCode(200).body("status", is("REFUNDED"));
        assertThat(Provider.refunds(), is(1));
    }

    @Test
    void aRefundWhoseAnswerIsLostIsRetriedWithTheSameKeyAndRefundsOnce() {
        String id = capture("rf-4b");
        Provider.loseRefundAnswers(1);

        Api.as(finance).post("/api/payments/" + id + "/refund").then().statusCode(200).body("status", is("REFUNDED"));
        assertThat("the provider refunded when it first heard, and answered the retry from its memory", Provider.refunds(), is(1));
    }

    @Test
    void aRefundThatCannotBeDoneLeavesThePaymentCapturedAndCanBeAskedAgain() {
        String id = capture("rf-5");
        Provider.down();

        Api.as(finance).post("/api/payments/" + id + "/refund").then().statusCode(503).body("code", is("PROVIDER_UNAVAILABLE"));
        Api.get(ana, id).then().body("status", is("CAPTURED"));

        Provider.up();
        Api.as(finance).post("/api/payments/" + id + "/refund").then().statusCode(200).body("status", is("REFUNDED"));
        assertThat(Provider.refunds(), is(1));
    }

    @Test
    void theCustomerWhoRetriesTheirPaymentAfterARefundSeesTheCurrentState() {
        String id = capture("rf-6");
        Api.as(finance).post("/api/payments/" + id + "/refund").then().statusCode(200);

        Api.pay(ana, "rf-6", Api.body(Api.CARD)).then().statusCode(201).body("status", is("REFUNDED"))
                .header("Idempotent-Replayed", is("true"));
        assertThat("and nobody is charged again", Provider.charges(), is(1));
    }
}
