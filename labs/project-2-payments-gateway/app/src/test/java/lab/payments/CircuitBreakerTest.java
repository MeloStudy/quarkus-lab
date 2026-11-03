package lab.payments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

/** When the provider stays down, stop asking it (lab 23): fail fast, keep the key free, keep everything that does not need it working. */
@QuarkusTest
@TestProfile(LogProfile.class)
class CircuitBreakerTest extends PaymentsTestBase {

    private final String ana = Tokens.customer(Api.ANA);
    private final String finance = Tokens.finance("emp_1");

    @Test
    void anOpenBreakerAnswers503WithoutAskingTheProviderAndWithoutRecordingAnything() {
        int recorded = openTheBreaker();
        int callsWhenItOpened = Provider.calls();

        Response response = Api.pay(ana, "after-open", Api.body(Api.CARD));

        response.then().statusCode(503).body("code", is("PROVIDER_UNAVAILABLE")).header("Retry-After", is("10"));
        assertThat("the provider was not asked", Provider.calls(), is(callsWhenItOpened));
        Api.as(finance).get("/api/payments").then().body("$", hasSize(recorded)); // only the UNKNOWN ones, nothing for the refused payments
    }

    @Test
    void theKeyOfAPaymentThatWasNeverSentCanBeUsedAgain() {
        openTheBreaker();
        Api.pay(ana, "kept-key", Api.body(Api.CARD)).then().statusCode(503);

        Provider.reset();
        waitForTheBreakerToTryAgain();

        Api.pay(ana, "kept-key", Api.body(Api.CARD)).then().statusCode(201).body("status", is("CAPTURED"));
        assertThat(Provider.charges(), is(1));
    }

    @Test
    void afterTheWaitOneProbeDecidesAndAHealthyProviderClosesTheBreaker() {
        openTheBreaker();
        Provider.reset();
        waitForTheBreakerToTryAgain();

        Api.pay(ana, "probe-1", Api.body(Api.CARD)).then().statusCode(201);
        Api.pay(ana, "probe-2", Api.body(Api.CARD)).then().statusCode(201);
        assertThat(Provider.charges(), is(2));
    }

    @Test
    void aProviderThatIsStillDownKeepsTheBreakerOpen() {
        openTheBreaker();
        waitForTheBreakerToTryAgain();

        Provider.failNextCharges(100, 503);
        Api.pay(ana, "probe-3", Api.body(Api.CARD)).then().statusCode(202); // the probe fails: UNKNOWN, and the breaker opens again
        Api.pay(ana, "probe-4", Api.body(Api.CARD)).then().statusCode(503);
    }

    @Test
    void readingKeepsWorkingWhileTheBreakerIsOpen() {
        String id = Api.pay(ana, "read-1", Api.body(Api.CARD)).then().statusCode(201).extract().path("id");
        openTheBreaker();

        Api.get(ana, id).then().statusCode(200);
        Api.as(ana).get("/api/payments").then().statusCode(200);
    }

    @Test
    void refundingStillWorksWhileTheBreakerOfChargesIsOpen() {
        String captured = Api.pay(ana, "mix-1", Api.body(Api.CARD)).then().statusCode(201).extract().path("id");
        openTheBreaker();
        // Only charges were scripted to fail, and the breaker guards charges only: a refund does not even notice.

        Api.as(finance).post("/api/payments/" + captured + "/refund").then().statusCode(200).body("status", is("REFUNDED"));
    }

    private static void waitForTheBreakerToTryAgain() {
        try {
            Thread.sleep(1300); // the breaker stays open 1 second in the tests (see application.properties)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
