package lab.clinic;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Sends the reminders that are due. NOT transactional: it claims in one transaction, calls the provider with none, and records
 * each answer in another (see {@link ReminderStore}). Safe to call from several instances at once.
 */
@ApplicationScoped
public class ReminderService {

    private static final Logger LOG = Logger.getLogger(ReminderService.class);

    /** What one run did. */
    public record Report(int claimed, int sent, int retrying, int failed, int postponed) {
    }

    private final ReminderStore store;
    private final ReminderGateway gateway;
    private final ClinicMetrics metrics;
    private final Tracer tracer;

    ReminderService(ReminderStore store, ReminderGateway gateway, ClinicMetrics metrics, Tracer tracer) {
        this.store = store;
        this.gateway = gateway;
        this.metrics = metrics;
        this.tracer = tracer;
    }

    public Report sendDue() {
        List<Long> ids = store.claim();
        int sent = 0;
        int retrying = 0;
        int failed = 0;
        int postponed = 0;
        boolean providerDown = false;
        for (long id : ids) {
            if (providerDown) {
                store.postpone(id); // the breaker is open: do not even try the rest of the batch
                postponed++;
                continue;
            }
            Optional<ReminderStore.Outgoing> outgoing = store.prepare(id);
            if (outgoing.isEmpty()) {
                continue; // it no longer applies
            }
            ReminderStore.Outgoing o = outgoing.get();
            // one span per reminder (the HTTP call to the provider becomes its child) and its ids on every log line of the block
            Span span = tracer.spanBuilder("reminder.send").setAttribute("reminder.id", o.reminderId())
                    .setAttribute("appointment.id", o.appointmentId()).startSpan();
            MDC.put("reminderId", o.reminderId());
            MDC.put("appointmentId", o.appointmentId());
            // Own keys (the names of the OpenTelemetry log model), set by hand: "traceId" and "spanId" belong to the Quarkus
            // integration, which clears them outside a request (measured: they never reached the log line of a job)
            MDC.put("trace_id", span.getSpanContext().getTraceId());
            MDC.put("span_id", span.getSpanContext().getSpanId());
            long started = System.nanoTime();
            String outcome = "sent";
            try (Scope ignored = span.makeCurrent()) {
                gateway.send(o.idempotencyKey(), o.to(), o.text());
                store.sent(o);
                sent++;
            } catch (CircuitBreakerOpenException e) {
                outcome = "breaker_open";
                store.postpone(id);
                postponed++;
                providerDown = true;
                LOG.warnf("The notification provider looks down (breaker open): the rest of the batch is put off");
            } catch (PermanentFailure e) {
                outcome = "refused";
                store.failedForGood(o, e.getMessage());
                failed++;
            } catch (TransientFailure | TimeoutException e) {
                outcome = "unavailable";
                store.failedTransiently(o, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                retrying++;
            } catch (RuntimeException e) {
                // something nobody planned: count it as an attempt and keep the batch going
                outcome = "unexpected";
                LOG.errorf(e, "Unexpected error sending reminder %d", id);
                store.failedTransiently(o, "unexpected: " + e.getClass().getSimpleName());
                retrying++;
            } finally {
                span.setAttribute("reminder.outcome", outcome);
                if (!"sent".equals(outcome)) {
                    span.setStatus(StatusCode.ERROR, outcome);
                }
                span.end();
                metrics.providerCalls(outcome).record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
                MDC.remove("reminderId");
                MDC.remove("appointmentId");
                MDC.remove("trace_id");
                MDC.remove("span_id");
            }
        }
        Report report = new Report(ids.size(), sent, retrying, failed, postponed);
        metrics.remindersRun(report);
        return report;
    }
}
