package lab.melostudy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

/** The trap of Task 4: a trace id in the log does not mean the trace exists anywhere. */
@QuarkusTest
@TestProfile(SamplingProfile.class)
class SamplingTest extends LabTestBase {

    @Inject
    InMemorySpanExporter exporter;

    @Test
    void anUnsampledRequestKeepsNoSpansButStillHasATraceIdInTheLog() throws Exception {
        exporter.reset();
        Stub.reset();
        int mark = Logs.mark();
        order("A1", "unsampled-1").then().statusCode(201);

        List<JsonNode> lines = Logs.app(Logs.await(mark, r -> Logs.app(r).size() >= 2));
        JsonNode created = lines.stream().filter(x -> Logs.text(x, "message").startsWith("Order ")).findFirst().orElseThrow();
        assertThat("the log line has a trace id", Logs.text(created, "traceId"), is(notNullValue()));
        assertThat(Logs.text(created, "sampled"), is("false"));

        Thread.sleep(300);
        assertThat("nothing was recorded", exporter.getFinishedSpanItems(), is(empty()));

        // The decision travels with the call: the sampled bit of the trace flags is off for the stock service too.
        String traceparent = (String) Stub.requests().get(0).get("traceparent");
        int flags = Integer.parseInt(traceparent.substring(traceparent.lastIndexOf('-') + 1), 16);
        assertThat(flags & 1, is(0));
    }
}
