package lab.payments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import jakarta.inject.Inject;

/** A payment is one trace (lab 25): the request, the business step, every attempt at the provider, and the logs that point at it. */
@QuarkusTest
@TestProfile(LogProfile.class)
class TracingTest extends PaymentsTestBase {

    private static final AttributeKey<String> PAYMENT_ID = AttributeKey.stringKey("payment.id");
    private static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("payment.outcome");

    @Inject
    InMemorySpanExporter exporter;

    private final String ana = Tokens.customer(Api.ANA);

    @BeforeEach
    void clean() {
        exporter.reset();
    }

    private List<SpanData> spans(Predicate<List<SpanData>> ready) {
        long deadline = System.currentTimeMillis() + 3000;
        List<SpanData> spans = exporter.getFinishedSpanItems();
        while (!ready.test(spans) && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            spans = exporter.getFinishedSpanItems();
        }
        return spans;
    }

    private List<SpanData> spansOfTheRequest() {
        return spans(all -> all.stream().anyMatch(s -> s.getKind() == SpanKind.SERVER));
    }

    private static SpanData named(List<SpanData> spans, String name) {
        return spans.stream().filter(s -> s.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("no span named " + name + " in " + spans.stream().map(SpanData::getName).toList()));
    }

    private static List<SpanData> ofKind(List<SpanData> spans, SpanKind kind) {
        return spans.stream().filter(s -> s.getKind() == kind).toList();
    }

    @Test
    void aPaymentIsOneTraceFromTheRequestToTheProvider() {
        Response response = Api.pay(ana, "t-1", Api.body(Api.CARD));
        response.then().statusCode(201);
        String id = response.path("id");

        List<SpanData> spans = spansOfTheRequest();
        SpanData server = named(spans, "POST /api/payments");
        SpanData business = named(spans, "payment.create");
        SpanData client = ofKind(spans, SpanKind.CLIENT).get(0);
        assertThat(business.getTraceId(), is(server.getTraceId()));
        assertThat(client.getTraceId(), is(server.getTraceId()));
        assertThat(business.getParentSpanId(), is(server.getSpanId()));
        assertThat(client.getParentSpanId(), is(business.getSpanId()));
        assertThat(business.getAttributes().get(PAYMENT_ID), is(id));
        assertThat(business.getAttributes().get(OUTCOME), is("captured"));
    }

    @Test
    void theProviderReceivesTheTraceContextAndTheRequestIdOfTheSameRequest() {
        Api.as(ana).header("Idempotency-Key", "t-2").header("X-Request-Id", "trace-req-2").body(Api.body(Api.CARD)).post("/api/payments")
                .then().statusCode(201);

        List<SpanData> spans = spansOfTheRequest();
        SpanData client = ofKind(spans, SpanKind.CLIENT).get(0);
        String traceparent = (String) Provider.seen().get(0).get("traceparent");
        assertThat("a traceparent reached the provider", traceparent, is(notNullValue()));
        assertThat(traceparent, containsString("-" + client.getTraceId() + "-"));
        assertThat(traceparent, containsString("-" + client.getSpanId() + "-"));
        assertThat(Provider.seen().get(0).get("requestId"), is("trace-req-2"));
    }

    @Test
    void everyRetryIsAnAttemptOfItsOwnInTheTraceAndTheFailedOnesAreMarked() {
        Provider.failNextCharges(2, 503);

        Api.pay(ana, "t-3", Api.body(Api.CARD)).then().statusCode(201);

        List<SpanData> spans = spans(all -> ofKind(all, SpanKind.CLIENT).size() >= 3 && all.stream().anyMatch(s -> s.getKind() == SpanKind.SERVER));
        List<SpanData> attempts = ofKind(spans, SpanKind.CLIENT);
        assertThat(attempts.size(), is(3));
        assertThat(attempts.stream().filter(s -> s.getStatus().getStatusCode() == StatusCode.ERROR).count(), is(2L));
        assertThat("all attempts hang from the same business step", attempts.stream().map(SpanData::getParentSpanId).distinct().count(), is(1L));
    }

    @Test
    void anUnknownOutcomeIsInTheTraceAsAnAttributeAndIsNotAnErrorOfTheRequest() {
        Provider.failNextCharges(10, 503);

        Api.pay(ana, "t-4", Api.body(Api.CARD)).then().statusCode(202);

        List<SpanData> spans = spansOfTheRequest();
        assertThat(named(spans, "payment.create").getAttributes().get(OUTCOME), is("unknown"));
        assertThat("the request was served: 202 is not a failure", named(spans, "POST /api/payments").getStatus().getStatusCode(),
                is(not(StatusCode.ERROR)));
    }

    @Test
    void aPaymentThatWasNeverSentHasNoCallToTheProviderInItsTrace() {
        openTheBreaker();
        exporter.reset();

        Api.pay(ana, "t-5", Api.body(Api.CARD)).then().statusCode(503);

        List<SpanData> spans = spansOfTheRequest();
        assertThat(named(spans, "payment.create").getAttributes().get(OUTCOME), is("not_attempted"));
        assertThat("no attempt, no client span", ofKind(spans, SpanKind.CLIENT).size(), is(0));
        assertThat("a 503 is the service failing to serve", named(spans, "POST /api/payments").getStatus().getStatusCode(), is(StatusCode.ERROR));
    }

    @Test
    void logLinesCarryTheIdOfTheirTrace() {
        int mark = Logs.mark();
        Api.as(ana).header("Idempotency-Key", "t-6").header("X-Request-Id", "trace-req-6").body(Api.body(Api.CARD)).post("/api/payments")
                .then().statusCode(201);

        String traceId = named(spansOfTheRequest(), "POST /api/payments").getTraceId();
        List<JsonNode> lines = Logs.app(Logs.await(mark, r -> Logs.app(r).size() >= 1));
        assertThat(lines.isEmpty(), is(false));
        for (JsonNode line : lines) {
            assertThat(Logs.text(line, "traceId"), is(traceId));
            assertThat(Logs.text(line, "requestId"), is("trace-req-6"));
        }
    }

    @Test
    void theServiceIsCalledPayments() {
        Api.pay(ana, "t-7", Api.body(Api.CARD)).then().statusCode(201);

        SpanData server = named(spansOfTheRequest(), "POST /api/payments");
        assertThat(server.getResource().getAttribute(AttributeKey.stringKey("service.name")), is("payments"));
    }

    @Test
    void reconcilingAndRefundingAreTracedToo() {
        String id = Api.pay(ana, "t-8", Api.body("tok_slowcapture1")).then().statusCode(202).extract().path("id");
        exporter.reset();

        Api.as(Tokens.finance("emp_1")).post("/api/payments/" + id + "/reconcile").then().statusCode(200);

        List<SpanData> spans = spans(all -> all.stream().anyMatch(s -> s.getName().equals("payment.reconcile")) && all.stream().anyMatch(s -> s.getKind() == SpanKind.SERVER));
        assertThat(named(spans, "payment.reconcile").getAttributes().get(PAYMENT_ID), is(id));
        assertThat(ofKind(spans, SpanKind.CLIENT).size(), is(1));
    }
}
