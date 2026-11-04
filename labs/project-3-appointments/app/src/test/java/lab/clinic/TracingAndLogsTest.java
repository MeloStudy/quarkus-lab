package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;

/**
 * A reminder is one trace (lab 25): the business step, the call to the provider as its child, the provider receives the trace id,
 * and the log lines of the step carry the same ids. A trace id in a log proves nothing by itself - this test looks for the span.
 */
@QuarkusTest
@QuarkusTestResource(NotificationStub.class)
@TestProfile(LogProfile.class)
@TestSecurity(user = "front-desk", roles = "reception")
class TracingAndLogsTest {

    private static final AttributeKey<Long> REMINDER_ID = AttributeKey.longKey("reminder.id");
    private static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("reminder.outcome");

    @Inject
    TestData data;

    @Inject
    ReminderService reminders;

    @Inject
    InMemorySpanExporter exporter;

    long ana;
    long checkup;

    @BeforeEach
    void fixture() {
        data.reset();
        Provider.reset();
        CircuitBreakerMaintenance.get().resetAll();
        exporter.reset();
        Clinic.clockAt(Clinic.NOW);
        ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        checkup = Clinic.treatment("Checkup", 30);
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

    private SpanData reminderSpan() {
        return spans(all -> all.stream().anyMatch(s -> s.getName().equals("reminder.send"))).stream()
                .filter(s -> s.getName().equals("reminder.send")).findFirst().orElseThrow();
    }

    @Test
    void aReminderIsOneTraceAndTheProviderReceivesItsId() {
        long bea = Clinic.patient("Bea", "bea@example.com");
        Clinic.booked(ana, bea, checkup, "2026-03-02T10:00:00Z");
        exporter.reset(); // forget the spans of the booking request: this test is about the reminder

        assertEquals(1, reminders.sendDue().sent());

        SpanData step = reminderSpan();
        assertEquals(1L, step.getAttributes().get(REMINDER_ID));
        assertEquals("sent", step.getAttributes().get(OUTCOME));
        SpanData call = spans(all -> all.stream().anyMatch(s -> s.getKind() == SpanKind.CLIENT)).stream()
                .filter(s -> s.getKind() == SpanKind.CLIENT).findFirst().orElseThrow();
        assertEquals(step.getSpanId(), call.getParentSpanId(), "the call to the provider is a child of the reminder step");
        assertEquals(step.getTraceId(), call.getTraceId());
        assertTrue(Provider.traceparents().get(0).contains(step.getTraceId()), "the provider received our trace id: " + Provider.traceparents());
    }

    @Test
    void aRefusedReminderIsAnErrorSpanAndItsLogLineCarriesTheSameIds() {
        long nobody = Clinic.patient("Nobody", "nobody@invalid.test");
        long appointment = Clinic.booked(ana, nobody, checkup, "2026-03-02T10:00:00Z");
        exporter.reset();
        int mark = Logs.mark();

        reminders.sendDue();

        SpanData step = reminderSpan();
        assertEquals("refused", step.getAttributes().get(OUTCOME));
        assertEquals(StatusCode.ERROR, step.getStatus().getStatusCode());

        List<JsonNode> records = Logs.await(mark, r -> Logs.app(r).stream().anyMatch(x -> String.valueOf(Logs.text(x, "message")).contains("refused for good")));
        JsonNode line = Logs.app(records).stream().filter(x -> String.valueOf(Logs.text(x, "message")).contains("refused for good"))
                .findFirst().orElseThrow(() -> new AssertionError("no log line about the refusal in " + records));
        assertEquals("1", Logs.text(line, "reminderId"));
        assertEquals(String.valueOf(appointment), Logs.text(line, "appointmentId"));
        assertEquals(step.getTraceId(), Logs.text(line, "trace_id"), "the log line points at the trace that exists");
        assertNotNull(Logs.text(line, "span_id"));
    }

    @Test
    void theLogsNeverContainTheEmailOfThePatient() {
        long nobody = Clinic.patient("Nobody", "nobody@invalid.test");
        Clinic.booked(ana, nobody, checkup, "2026-03-02T10:00:00Z");
        int mark = Logs.mark();

        reminders.sendDue();
        Logs.await(mark, r -> Logs.app(r).stream().anyMatch(x -> String.valueOf(Logs.text(x, "message")).contains("refused for good")));

        assertFalse(String.join("\n", Logs.rawLinesSince(mark)).contains("nobody@invalid.test"), "personal data stays out of the logs");
        given().get("/api/reminders").then().statusCode(200);
    }
}
