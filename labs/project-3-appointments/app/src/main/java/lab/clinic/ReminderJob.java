package lab.clinic;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The clock of the reminders: every tick sends what is due. {@code SKIP}: a slow tick is not started again on top of itself on
 * this instance (several instances are safe anyway: the claim uses SKIP LOCKED). Lab 13 explains the cluster caveat.
 */
@ApplicationScoped
public class ReminderJob {

    private static final Logger LOG = Logger.getLogger(ReminderJob.class);

    private final ReminderService reminders;

    ReminderJob(ReminderService reminders) {
        this.reminders = reminders;
    }

    @Scheduled(every = "${clinic.reminders.poll-interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP, identity = "reminders")
    void tick() {
        ReminderService.Report report = reminders.sendDue();
        if (report.claimed() > 0) {
            LOG.infof("Reminders: %s", report);
        }
    }
}
