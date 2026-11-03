package lab.payments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

/** What the logs say (lab 25), for a service whose log lines are read by people at 3 a.m. and kept for months. */
@QuarkusTest
@TestProfile(LogProfile.class)
class LoggingTest extends PaymentsTestBase {

    private final String ana = Tokens.customer(Api.ANA);

    private static boolean isProblem(JsonNode record) {
        String level = Logs.text(record, "level");
        return "WARN".equals(level) || "ERROR".equals(level);
    }

    private static List<JsonNode> ours(int mark, int atLeast) {
        return Logs.app(Logs.await(mark, r -> Logs.app(r).size() >= atLeast));
    }

    private Response payWithId(String key, String token, String requestId) {
        return Api.as(ana).header("Idempotency-Key", key).header("X-Request-Id", requestId).body(Api.body(token)).post("/api/payments");
    }

    @Test
    void everyLineOfAPaymentCarriesItsRequestIdAsAJsonField() {
        int mark = Logs.mark();
        payWithId("l-1", Api.CARD, "log-req-1").then().statusCode(201);

        List<JsonNode> lines = ours(mark, 1);
        assertThat(lines, is(not(empty())));
        for (JsonNode line : lines) {
            assertThat(Logs.text(line, "message"), Logs.text(line, "requestId"), is("log-req-1"));
        }
    }

    @Test
    void anErrorTellsTheCustomerTheSameIdThatTheLogsUse() {
        Response response = Api.as(ana).header("X-Request-Id", "log-req-2").body("{\"amount\":0}").post("/api/payments");

        response.then().statusCode(400).header("X-Request-Id", is("log-req-2")).body("requestId", is("log-req-2"));
    }

    @Test
    void aDeclineIsABusinessOutcomeNotAWarning() {
        payWithId("l-3-warm", Api.CARD, "warm").then().statusCode(201);
        int mark = Logs.mark();
        payWithId("l-3", "tok_declined1", "log-req-3").then().statusCode(402);

        List<JsonNode> lines = ours(mark, 1);
        assertThat("the decline was logged", lines, is(not(empty())));
        assertThat(lines.stream().filter(LoggingTest::isProblem).toList(), is(empty()));
    }

    @Test
    void anUnknownPaymentLeavesAWarningThatNamesItAndPointsAtTheRequest() {
        int mark = Logs.mark();
        String id = payWithId("l-4", "tok_broken123", "log-req-4").then().statusCode(202).extract().path("id");

        List<JsonNode> lines = Logs.await(mark, r -> r.stream().anyMatch(LoggingTest::isProblem));
        JsonNode warning = lines.stream().filter(LoggingTest::isProblem).findFirst().orElse(null);
        assertThat("an UNKNOWN payment must not pass in silence", warning, is(notNullValue()));
        assertThat(Logs.text(warning, "level"), is("WARN"));
        assertThat(Logs.text(warning, "message"), containsString(id));
        assertThat(Logs.text(warning, "requestId"), is("log-req-4"));
    }

    @Test
    void aPaymentThatWasNeverSentLeavesAWarningToo() {
        Provider.down();
        int mark = Logs.mark();
        payWithId("l-5", Api.CARD, "log-req-5").then().statusCode(503);

        List<JsonNode> lines = Logs.await(mark, r -> r.stream().anyMatch(LoggingTest::isProblem));
        assertThat(lines.stream().filter(LoggingTest::isProblem).map(l -> Logs.text(l, "level")).toList(), hasItem("WARN"));
    }

    @Test
    void aRejectionByTheProviderIsAnErrorBecauseSomethingOfOursIsWrong() {
        int mark = Logs.mark();
        payWithId("l-6", "tok_rejected1", "log-req-6").then().statusCode(502);

        List<JsonNode> lines = Logs.await(mark, r -> r.stream().anyMatch(l -> "ERROR".equals(Logs.text(l, "level"))));
        assertThat(lines.stream().map(l -> Logs.text(l, "level")).toList(), hasItem("ERROR"));
    }

    @Test
    void anUnplannedFailureIsLoggedWithItsErrorIdAndShowsTheCustomerOnlyThatId() {
        int mark = Logs.mark();
        Response response = Api.as(ana).header("X-Request-Id", "log-req-7").get("/api/_probe/boom");

        response.then().statusCode(500).body("code", is("INTERNAL_ERROR"));
        String message = response.path("message");
        Matcher errorId = Pattern.compile("error ([0-9a-f-]{36})").matcher(message);
        assertThat(errorId.find(), is(true));
        assertThat(response.asString(), not(containsString("jdbc")));
        assertThat(response.asString(), not(containsString("IllegalStateException")));

        List<JsonNode> lines = Logs.await(mark, r -> r.stream().anyMatch(l -> "ERROR".equals(Logs.text(l, "level"))));
        JsonNode error = lines.stream().filter(l -> "ERROR".equals(Logs.text(l, "level"))).findFirst().orElseThrow();
        assertThat(Logs.text(error, "message"), containsString(errorId.group(1)));
        assertThat(Logs.text(error, "requestId"), is("log-req-7"));
    }

    @Test
    void neitherCardsNorCredentialsNorTokensEverReachTheLog() {
        String pan = "4111111111111111";
        int mark = Logs.mark();

        Api.pay(ana, "l-8a", Api.body(Api.CARD)).then().statusCode(201);
        Api.pay(ana, "l-8b", Api.body("tok_declined1")).then().statusCode(402);
        Api.pay(ana, "l-8d", Api.body("tok_rejected1")).then().statusCode(502);
        Api.pay(ana, "l-8c", Api.body("tok_broken123")).then().statusCode(202); // opens the breaker: anything after it is not sent
        Api.pay(ana, "l-8e", "{\"amount\":100,\"currency\":\"EUR\",\"cardToken\":\"" + pan + "\"}").then().statusCode(400);
        Api.pay(Tokens.forged(Api.ANA, "customer"), "l-8f", Api.body(Api.CARD)).then().statusCode(401);
        Api.get(ana, "pay_nothing").then().statusCode(404);
        Logs.await(mark, r -> Logs.app(r).size() >= 8);

        String log = String.join("\n", Logs.rawLinesSince(mark));
        assertThat("the log has the lines of the payments", log, containsString("is CAPTURED"));
        assertThat(log, not(containsString("tok_")));
        assertThat(log, not(containsString(pan)));
        assertThat(log, not(containsString(ProviderStub.API_KEY)));
        assertThat(log, not(containsString("Bearer")));
        assertThat("no JWT", log, not(containsString("eyJ")));
        assertThat("no customer id", log, not(containsString(Api.ANA)));
    }
}
