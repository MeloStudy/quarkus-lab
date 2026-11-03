package lab.payments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

/** A "no" from the provider is an answer, not a failure of the provider: it is final, it is not retried and it never counts against the breaker. */
@QuarkusTest
@TestProfile(LogProfile.class)
class DeclineTest extends PaymentsTestBase {

    private final String ana = Tokens.customer(Api.ANA);

    @Test
    void aDeclinedCardIs402WithTheReasonAndThePaymentThatRecordsIt() {
        Api.pay(ana, "d-0", Api.body(Api.CARD)).then().statusCode(201); // a good card works
        Provider.reset();

        Response response = Api.pay(ana, "d-1", Api.body("tok_declined1"));

        response.then().statusCode(402).body("code", is("CARD_DECLINED")).body("message", containsString("card_declined"))
                .body("paymentId", matchesPattern("pay_[0-9a-f]{20}"));
        String id = response.path("paymentId");
        Api.get(ana, id).then().statusCode(200).body("status", is("DECLINED")).body("declineReason", is("card_declined"));
    }

    @Test
    void aDeclineIsAskedOnceAndNeverRetried() {
        Api.pay(ana, "d-2", Api.body("tok_declined1")).then().statusCode(402);

        assertThat(Provider.calls(), is(1));
        assertThat("nobody was charged", Provider.charges(), is(0));
    }

    @Test
    void manyDeclinesDoNotOpenTheBreakerBecauseCustomersHaveBadCards() {
        for (int i = 0; i < 12; i++) {
            Api.pay(ana, "d-3-" + i, Api.body("tok_declined" + i)).then().statusCode(402);
        }

        Api.pay(ana, "d-3-good", Api.body(Api.CARD)).then().statusCode(201);
        assertThat("each decline was one call, the good payment another", Provider.calls(), is(13));
    }

    @Test
    void whenTheProviderRefusesOurRequestNobodyWasChargedAndSomebodyMustFixIt() {
        Response response = Api.pay(ana, "d-4", Api.body("tok_rejected1"));

        response.then().statusCode(502).body("code", is("PROVIDER_REJECTED"));
        String id = response.path("paymentId");
        Api.get(ana, id).then().body("status", is("FAILED"));
        assertThat("a rejection is not retried", Provider.calls(), is(1));
    }

    @Test
    void rejectionsDoNotOpenTheBreakerEither() {
        for (int i = 0; i < 8; i++) {
            Api.pay(ana, "d-5-" + i, Api.body("tok_rejected" + i)).then().statusCode(502);
        }

        Api.pay(ana, "d-5-good", Api.body(Api.CARD)).then().statusCode(201);
    }
}
