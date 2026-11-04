package lab.clinic;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * What the clinic measures, named after the business and not after the framework. Every tag is a value from a short fixed list
 * (an outcome, a code of the error contract): never an id, an email or a time - that would create a new series for every request.
 */
@ApplicationScoped
public class ClinicMetrics {

    private final MeterRegistry registry;
    private final ReminderStore reminders;

    ClinicMetrics(MeterRegistry registry, ReminderStore reminders) {
        this.registry = registry;
        this.reminders = reminders;
    }

    /** Something happened to an appointment: booked, rescheduled, cancelled, completed, no_show. */
    public void appointment(String outcome) {
        Counter.builder("clinic.appointments").description("Appointments by what happened to them").tag("outcome", outcome)
                .register(registry).increment();
    }

    /** A request the clinic refused for a rule of its own (409, 422), by the code of the error contract. */
    public void rejected(String code) {
        Counter.builder("clinic.requests.rejected").description("Requests refused by a rule of the clinic").tag("code", code)
                .register(registry).increment();
    }

    /** One run of the reminder job: how many ended in each way. */
    public void remindersRun(ReminderService.Report report) {
        count("sent", report.sent());
        count("retrying", report.retrying());
        count("failed", report.failed());
        count("postponed", report.postponed());
    }

    private void count(String result, int n) {
        if (n > 0) {
            Counter.builder("clinic.reminders").description("Reminders processed, by result").tag("result", result)
                    .register(registry).increment(n);
        }
    }

    /** The time of one call to the notification provider, by how it ended. */
    public Timer providerCalls(String outcome) {
        return Timer.builder("clinic.notifications.call").description("Calls to the notification provider")
                .tag("outcome", outcome).publishPercentileHistogram(false).register(registry);
    }

    /**
     * The gauges that say "somebody has to look at this": reminders that gave up, and how long the oldest overdue one has been
     * waiting. They query the database when they are read (a scrape), which is cheap here and would be cached in a bigger table.
     */
    @ApplicationScoped
    public static class Gauges {

        void register(@Observes StartupEvent event, MeterRegistry registry, ReminderStore store) {
            Gauge.builder("clinic.reminders.failed", store, s -> s.count(ReminderStatus.FAILED))
                    .description("Reminders that gave up and wait for a person").register(registry);
            Gauge.builder("clinic.reminders.pending", store, s -> s.count(ReminderStatus.PENDING))
                    .description("Reminders waiting to be sent").register(registry);
            Gauge.builder("clinic.reminders.overdue.seconds", store, s -> s.oldestOverdue().toSeconds())
                    .description("Age of the oldest pending reminder that should already have gone out").register(registry);
        }
    }
}
