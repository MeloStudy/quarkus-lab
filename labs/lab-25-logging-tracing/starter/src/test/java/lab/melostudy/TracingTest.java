package lab.melostudy;

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
import jakarta.inject.Inject;

/** Task 4: a request becomes a trace, and the trace and the logs point at each other. */
@QuarkusTest
@TestProfile(LogProfile.class)
class TracingTest extends LabTestBase {

    @Inject
    InMemorySpanExporter exporter;

    @BeforeEach
    void clean() {
        exporter.reset();
        Stub.reset();
    }

    /** The server span ends when the response has been written, which can be a moment after the test has it. */
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

    private List<SpanData> spansOfOneOrder() {
        return spans(all -> all.stream().anyMatch(s -> s.getKind() == SpanKind.SERVER));
    }

    private static SpanData server(List<SpanData> spans) {
        return spans.stream().filter(s -> s.getKind() == SpanKind.SERVER).findFirst()
                .orElseThrow(() -> new AssertionError("no server span"));
    }

    private static SpanData client(List<SpanData> spans) {
        return spans.stream().filter(s -> s.getKind() == SpanKind.CLIENT).findFirst()
                .orElseThrow(() -> new AssertionError("no client span"));
    }

    @Test
    void oneRequestIsOneTraceWithTheCallToTheStockService() {
        order("A1").then().statusCode(201);

        List<SpanData> spans = spansOfOneOrder();
        assertThat(server(spans).getName(), is("POST /api/orders"));
        assertThat(client(spans).getName(), is("POST /reserve"));
        assertThat(client(spans).getTraceId(), is(server(spans).getTraceId()));
    }

    @Test
    void theStockServiceReceivesTheTraceContext() {
        order("A1").then().statusCode(201);

        List<SpanData> spans = spansOfOneOrder();
        String traceparent = (String) Stub.requests().get(0).get("traceparent");
        assertThat("no traceparent header reached the stock service", traceparent, is(notNullValue()));
        assertThat(traceparent, containsString("-" + server(spans).getTraceId() + "-"));
        assertThat(traceparent, containsString("-" + client(spans).getSpanId() + "-"));
    }

    @Test
    void logLinesCarryTheIdOfTheirTrace() {
        int mark = Logs.mark();
        order("A1", "traced-1").then().statusCode(201);

        String traceId = server(spansOfOneOrder()).getTraceId();
        List<JsonNode> lines = Logs.app(Logs.await(mark, r -> Logs.app(r).size() >= 2));
        JsonNode created = lines.stream().filter(x -> Logs.text(x, "message").startsWith("Order ")).findFirst().orElseThrow();
        assertThat(Logs.text(created, "traceId"), is(traceId));
        assertThat(Logs.text(created, "spanId"), is(notNullValue()));
        assertThat(Logs.text(created, "requestId"), is("traced-1"));
    }

    @Test
    void aTraceStartedByTheCallerContinues() {
        request().header("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01")
                .body(body("A1")).post("/api/orders").then().statusCode(201);

        SpanData server = server(spansOfOneOrder());
        assertThat(server.getTraceId(), is("0af7651916cd43dd8448eb211c80319c"));
        assertThat(server.getParentSpanId(), is("b7ad6b7169203331"));
    }

    @Test
    void theBusinessStepIsASpanBetweenTheRequestAndTheCall() {
        order("A1").then().statusCode(201);

        List<SpanData> spans = spans(all -> all.size() >= 3);
        SpanData step = spans.stream().filter(s -> s.getName().equals("reserve-stock")).findFirst()
                .orElseThrow(() -> new AssertionError("no span named reserve-stock in " + spans.stream().map(SpanData::getName).toList()));
        assertThat(step.getAttributes().get(AttributeKey.stringKey("order.sku")), is("A1"));
        assertThat(step.getParentSpanId(), is(server(spans).getSpanId()));
        assertThat(client(spans).getParentSpanId(), is(step.getSpanId()));
    }

    @Test
    void theServiceHasAName() {
        order("A1").then().statusCode(201);

        SpanData server = server(spansOfOneOrder());
        assertThat(server.getResource().getAttribute(AttributeKey.stringKey("service.name")), is("orders"));
    }

    @Test
    void aFailureOfOursMarksTheTrace() {
        order("BROKEN").then().statusCode(502);

        assertThat(server(spansOfOneOrder()).getStatus().getStatusCode(), is(StatusCode.ERROR));
    }

    @Test
    void aRefusalIsNotAFailure() {
        order("SOLD-OUT").then().statusCode(409);

        assertThat(server(spansOfOneOrder()).getStatus().getStatusCode(), is(not(StatusCode.ERROR)));
    }
}
