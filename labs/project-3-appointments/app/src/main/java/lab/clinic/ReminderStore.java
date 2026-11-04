package lab.clinic;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Every database operation of the reminders, each in its OWN short transaction. The call to the provider happens outside
 * any of them (a connection held while waiting for a remote service is how a pool runs dry, lab 23): the job claims, then
 * calls, then records.
 *
 * <p>The recording methods only act on a reminder that is still PENDING and whose appointment is still at the version that
 * was sent. If somebody rescheduled or cancelled in the meantime, the old answer is stale and is dropped.
 */
@ApplicationScoped
public class ReminderStore {

    private static final Logger LOG = Logger.getLogger(ReminderStore.class);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE d MMMM 'at' HH:mm", Locale.ENGLISH);

    /** What to send for one picked reminder; {@code appointmentVersion} identifies the moment the message describes. */
    public record Outgoing(long reminderId, long appointmentId, long appointmentVersion, String idempotencyKey, String to, String text) {
    }

    private final EntityManager em;
    private final Clock clock;
    private final ReminderConfig config;
    private final ClinicConfig clinic;

    ReminderStore(EntityManager em, Clock clock, ReminderConfig config, ClinicConfig clinic) {
        this.em = em;
        this.clock = clock;
        this.config = config;
        this.clinic = clinic;
    }

    // ------------------------------------------------------------------ called inside the booking transactions

    @Transactional
    public void schedule(Appointment a) {
        Reminder r = new Reminder();
        r.appointment = a;
        r.dueAt = dueFor(a);
        r.nextAttemptAt = r.dueAt;
        em.persist(r);
    }

    /** The appointment moved: the patient must be told the NEW time, so a sent reminder becomes pending again. */
    @Transactional
    public void reschedule(Appointment a) {
        Reminder r = byAppointment(a.id);
        r.status = ReminderStatus.PENDING;
        r.dueAt = dueFor(a);
        r.nextAttemptAt = r.dueAt;
        r.attempts = 0;
        r.lastError = null;
        r.sentAt = null;
    }

    /** Cancelled, completed or missed: a reminder that has not gone out yet no longer should. A sent one stays as history. */
    @Transactional
    public void cancelPending(Appointment a) {
        Reminder r = byAppointment(a.id);
        if (r.status == ReminderStatus.PENDING) {
            r.status = ReminderStatus.CANCELLED;
        }
    }

    private Instant dueFor(Appointment a) {
        Instant due = a.startsAt.minus(config.lead());
        Instant now = clock.instant();
        return due.isBefore(now) ? now : due;
    }

    private Reminder byAppointment(long appointmentId) {
        return em.createQuery("select r from Reminder r where r.appointment.id = :id", Reminder.class)
                .setParameter("id", appointmentId).getSingleResult();
    }

    // ------------------------------------------------------------------------------------ called by the job

    /**
     * Picks the reminders that are due and makes them ours for a while. {@code FOR UPDATE SKIP LOCKED} lets several
     * instances run this at the same time without ever picking the same row; the lease (next_attempt_at pushed ahead)
     * keeps the row ours after this transaction ends.
     */
    @Transactional
    public List<Long> claim() {
        Instant now = clock.instant();
        List<?> rows = em.createNativeQuery("""
                UPDATE reminder SET next_attempt_at = ?1
                WHERE id IN (SELECT id FROM reminder WHERE status = 'PENDING' AND next_attempt_at <= ?2
                             ORDER BY next_attempt_at, id LIMIT ?3 FOR UPDATE SKIP LOCKED)
                RETURNING id""")
                .setParameter(1, now.plus(config.lease())).setParameter(2, now).setParameter(3, config.batchSize()).getResultList();
        return rows.stream().map(row -> ((Number) row).longValue()).toList();
    }

    /** Builds the message, or finds out that it no longer applies (the appointment is not booked any more). */
    @Transactional
    public Optional<Outgoing> prepare(long reminderId) {
        Reminder r = em.createQuery("select r from Reminder r join fetch r.appointment a join fetch a.patient "
                + "join fetch a.practitioner join fetch a.treatment where r.id = :id", Reminder.class)
                .setParameter("id", reminderId).getSingleResult();
        if (r.status != ReminderStatus.PENDING) {
            return Optional.empty();
        }
        Appointment a = r.appointment;
        if (a.status != AppointmentStatus.BOOKED) {
            r.status = ReminderStatus.CANCELLED;
            return Optional.empty();
        }
        String text = "Reminder: " + a.treatment.name + " with " + a.practitioner.name + " on "
                + WHEN.format(a.startsAt.atZone(clinic.timeZone())) + " (" + clinic.timeZone().getId() + ")";
        return Optional.of(new Outgoing(r.id, a.id, a.version, "reminder-" + r.id + "-v" + a.version, a.patient.email, text));
    }

    @Transactional
    public void sent(Outgoing o) {
        current(o).ifPresent(r -> {
            r.status = ReminderStatus.SENT;
            r.sentAt = clock.instant();
            r.attempts++;
            r.lastError = null;
        });
    }

    /** "Not now": one more attempt is counted, and the next one waits twice as long as the last. */
    @Transactional
    public void failedTransiently(Outgoing o, String error) {
        current(o).ifPresent(r -> {
            r.attempts++;
            r.lastError = cut(error);
            if (r.attempts >= config.maxAttempts()) {
                r.status = ReminderStatus.FAILED;
                r.lastError = cut("gave up after " + r.attempts + " attempts: " + error);
                LOG.warnf("Reminder %d gave up after %d attempts: %s", r.id, r.attempts, error);
            } else {
                r.nextAttemptAt = clock.instant().plus(config.backoff().multipliedBy(1L << (r.attempts - 1)));
                LOG.warnf("Reminder %d attempt %d failed (%s); the next try is at %s", r.id, r.attempts, error, r.nextAttemptAt);
            }
        });
    }

    /** "Never": no retry. It waits in FAILED until a person decides what to do. */
    @Transactional
    public void failedForGood(Outgoing o, String error) {
        current(o).ifPresent(r -> {
            r.attempts++;
            r.status = ReminderStatus.FAILED;
            r.lastError = cut(error);
            LOG.warnf("Reminder %d refused for good: %s", r.id, error);
        });
    }

    /** The provider is known to be down: put it off WITHOUT counting an attempt, it is not this reminder's fault. */
    @Transactional
    public void postpone(long reminderId) {
        Reminder r = em.find(Reminder.class, reminderId);
        if (r != null && r.status == ReminderStatus.PENDING) {
            r.nextAttemptAt = clock.instant().plus(config.providerDownPause());
        }
    }

    // ------------------------------------------------------------------------------------ called by the API

    @Transactional
    public List<Reminder> list(ReminderStatus status) {
        if (status == null) {
            return em.createQuery("select r from Reminder r join fetch r.appointment order by r.id", Reminder.class).getResultList();
        }
        return em.createQuery("select r from Reminder r join fetch r.appointment where r.status = :status order by r.id",
                Reminder.class).setParameter("status", status).getResultList();
    }

    /** A person looked at a FAILED reminder and wants it tried again, from zero. */
    @Transactional
    public Reminder retry(long reminderId) {
        Reminder r = em.find(Reminder.class, reminderId);
        if (r == null) {
            throw ClinicException.notFound("Reminder " + reminderId + " does not exist");
        }
        if (r.status != ReminderStatus.FAILED) {
            throw ClinicException.conflict("INVALID_TRANSITION", "Only a failed reminder can be retried (this one is " + r.status + ")");
        }
        r.status = ReminderStatus.PENDING;
        r.attempts = 0;
        r.lastError = null;
        r.nextAttemptAt = clock.instant();
        return r;
    }

    private Optional<Reminder> current(Outgoing o) {
        Reminder r = em.find(Reminder.class, o.reminderId());
        if (r == null || r.status != ReminderStatus.PENDING || r.appointment.version != o.appointmentVersion()) {
            LOG.debugf("Dropping the stale answer for reminder %d", o.reminderId());
            return Optional.empty();
        }
        return Optional.of(r);
    }

    private static String cut(String text) {
        return text.length() <= 200 ? text : text.substring(0, 200);
    }

    /** Helper for tests and metrics: how many reminders are in each state. */
    @Transactional
    public long count(ReminderStatus status) {
        return em.createQuery("select count(r) from Reminder r where r.status = :s", Long.class).setParameter("s", status)
                .getSingleResult();
    }

    /** The age of the oldest reminder that is overdue and still pending, or zero. */
    @Transactional
    public Duration oldestOverdue() {
        Instant oldest = em.createQuery("select min(r.nextAttemptAt) from Reminder r where r.status = 'PENDING' and r.nextAttemptAt <= :now", Instant.class)
                .setParameter("now", clock.instant()).getSingleResult();
        return oldest == null ? Duration.ZERO : Duration.between(oldest, clock.instant());
    }
}
