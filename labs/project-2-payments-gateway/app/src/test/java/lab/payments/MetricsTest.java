package lab.payments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/** The numbers an operator needs (lab 24): outcomes, time spent at the provider, what is waiting for a human, and the breaker. */
@QuarkusTest
@TestProfile(LogProfile.class)
class MetricsTest extends PaymentsTestBase {

    private final String ana = Tokens.customer(Api.ANA);

    private static double created(String outcome) {
        return Ops.sample(Ops.metrics(), "payments_created_total", ".*outcome=\"" + outcome + "\".*");
    }

    private static double timed(String operation, String outcome) {
        return Ops.sample(Ops.metrics(), "payments_provider_duration_seconds_count",
                ".*operation=\"" + operation + "\".*outcome=\"" + outcome + "\".*");
    }

    private static double unknown() {
        return Ops.sample(Ops.metrics(), "payments_unknown", ".*");
    }

    @Test
    void everyOutcomeOfAPaymentIsCounted() {
        double captured = created("captured");
        double declined = created("declined");
        double unknown = created("unknown");
        double failed = created("failed");
        double notAttempted = created("not_attempted");

        Api.pay(ana, "m-1", Api.body(Api.CARD)).then().statusCode(201);
        Api.pay(ana, "m-2", Api.body("tok_declined1")).then().statusCode(402);
        Api.pay(ana, "m-3", Api.body("tok_rejected1")).then().statusCode(502);
        // From here on the provider fails: three failed attempts leave the payment UNKNOWN and open the breaker
        Api.pay(ana, "m-4", Api.body("tok_broken123")).then().statusCode(202);
        Provider.down();
        Api.pay(ana, "m-5", Api.body(Api.CARD)).then().statusCode(503);

        assertThat(created("captured"), is(captured + 1));
        assertThat(created("declined"), is(declined + 1));
        assertThat(created("unknown"), is(unknown + 1));
        assertThat(created("failed"), is(failed + 1));
        assertThat(created("not_attempted"), is(notAttempted + 1));
    }

    @Test
    void aReplayIsCountedApartFromACreation() {
        Api.pay(ana, "m-6", Api.body(Api.CARD)).then().statusCode(201);
        double created = created("captured");
        double replayed = Ops.sample(Ops.metrics(), "payments_replayed_total", ".*");

        Api.pay(ana, "m-6", Api.body(Api.CARD)).then().statusCode(201);

        assertThat(Ops.sample(Ops.metrics(), "payments_replayed_total", ".*"), is(replayed + 1));
        assertThat("a replay is not a new payment", created("captured"), is(created));
    }

    @Test
    void theTimeSpentAtTheProviderIsMeasuredPerOutcome() {
        double captured = timed("charge", "captured");
        double declined = timed("charge", "declined");

        Api.pay(ana, "m-7", Api.body(Api.CARD)).then().statusCode(201);
        Api.pay(ana, "m-8", Api.body("tok_declined1")).then().statusCode(402);

        assertThat(timed("charge", "captured"), is(captured + 1));
        assertThat(timed("charge", "declined"), is(declined + 1));
    }

    @Test
    void thePaymentsWaitingForAHumanAreAGaugeThatGoesDownWhenTheyAreSettled() {
        double before = unknown();
        String id = Api.pay(ana, "m-9", Api.body("tok_slowcapture1")).then().statusCode(202).extract().path("id");
        assertThat(unknown(), is(before + 1));

        Api.as(Tokens.finance("emp_1")).post("/api/payments/" + id + "/reconcile").then().statusCode(200);

        assertThat(unknown(), is(before));
    }

    @Test
    void theBreakerStateIsAMetricToAlertOn() throws InterruptedException {
        Api.pay(ana, "m-10", Api.body(Api.CARD)).then().statusCode(201);
        double opened = Ops.sample(Ops.metrics(), "ft_circuitbreaker_opened_total", ".*");

        openTheBreaker();

        String open = Ops.metrics();
        assertThat(Ops.sample(open, "ft_circuitbreaker_state_current", ".*state=\"open\".*"), is(1.0));
        assertThat(Ops.sample(open, "ft_circuitbreaker_opened_total", ".*"), is(opened + 1));

        // The provider recovers; after the wait one probe closes the breaker and the alert clears.
        Provider.reset();
        Thread.sleep(1300);
        Api.pay(ana, "m-11-probe", Api.body(Api.CARD)).then().statusCode(201);
        assertThat(Ops.sample(Ops.metrics(), "ft_circuitbreaker_state_current", ".*state=\"open\".*"), is(0.0));
    }

    @Test
    void everyMetricOfTheApplicationCarriesItsNameAndNoMetricCarriesAnIdentifier() {
        Api.pay(ana, "m-11", Api.body(Api.CARD)).then().statusCode(201);
        Api.pay(ana, "m-12", Api.body("tok_declined1")).then().statusCode(402);

        String metrics = Ops.metrics();
        Matcher application = Pattern.compile("^(payments_[a-z_]+)(\\{[^}]*\\})?", Pattern.MULTILINE).matcher(metrics);
        int checked = 0;
        while (application.find()) {
            checked++;
            assertThat(application.group(1) + " carries the common tag", String.valueOf(application.group(2)), org.hamcrest.Matchers.containsString("application=\"payments\""));
        }
        assertThat(checked > 5, is(true));

        // Every distinct tag value is a time series the monitoring system keeps forever: no payment, customer, key or token in tags.
        for (String line : metrics.split("\n")) {
            if (!line.startsWith("#")) {
                assertThat(line, not(org.hamcrest.Matchers.matchesPattern(".*=\"(pay_|cus_|tok_|ch_)[^\"]*\".*")));
                assertThat(line, not(org.hamcrest.Matchers.matchesPattern(".*[{,](customer|payment|payment_id|key|token|idempotency[a-z_]*)=.*")));
            }
        }
        assertThat(metrics, not(startsWith("payments_created_total{application=\"payments\",customer")));
    }
}
