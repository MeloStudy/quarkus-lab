package lab.clinic;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import lab.clinic.Repositories.PatientRepository;
import lab.clinic.Repositories.PractitionerRepository;
import lab.clinic.Repositories.TreatmentRepository;
import lab.clinic.Repositories.WorkingHoursRepository;
import lab.clinic.Requests.BookRequest;
import lab.clinic.Responses.AppointmentView;
import lab.clinic.Responses.PageView;
import lab.clinic.SlotCalculator.Block;

/**
 * Booking, rescheduling and the life of an appointment. The order of the checks is part of the contract:
 * existence (404) -> version (428, 412) -> state (409) -> shape of the time (422) -> the database (409).
 *
 * <p>Overlaps are NOT checked here. A "look, then insert" is a race between two requests; the exclusion
 * constraints of the schema are the only judge, and this class only translates their verdict.
 */
@ApplicationScoped
public class BookingService {

    private final AppointmentRepository appointments;
    private final PractitionerRepository practitioners;
    private final PatientRepository patients;
    private final TreatmentRepository treatments;
    private final WorkingHoursRepository hours;
    private final ClinicConfig config;
    private final Clock clock;
    private final Caller caller;
    private final ReminderStore reminders;
    private final ClinicMetrics metrics;

    BookingService(AppointmentRepository appointments, PractitionerRepository practitioners, PatientRepository patients,
            TreatmentRepository treatments, WorkingHoursRepository hours, ClinicConfig config, Clock clock, Caller caller,
            ReminderStore reminders, ClinicMetrics metrics) {
        this.appointments = appointments;
        this.practitioners = practitioners;
        this.patients = patients;
        this.treatments = treatments;
        this.hours = hours;
        this.config = config;
        this.clock = clock;
        this.caller = caller;
        this.reminders = reminders;
        this.metrics = metrics;
    }

    @Transactional
    public AppointmentView book(BookRequest request) {
        // who may book for whom comes before everything else, even before "does that patient exist"
        if (!caller.isReception() && !caller.patientId().filter(id -> id.equals(request.patientId())).isPresent()) {
            throw ClinicException.forbidden("You can only book appointments for yourself");
        }
        Practitioner practitioner = practitioners.findByIdOptional(request.practitionerId()).orElseThrow(
                () -> ClinicException.notFound("Practitioner " + request.practitionerId() + " does not exist"));
        Patient patient = patients.findByIdOptional(request.patientId())
                .orElseThrow(() -> ClinicException.notFound("Patient " + request.patientId() + " does not exist"));
        Treatment treatment = treatments.findByIdOptional(request.treatmentId())
                .orElseThrow(() -> ClinicException.notFound("Treatment " + request.treatmentId() + " does not exist"));

        Instant start = request.startsAt();
        Instant end = start.plus(Duration.ofMinutes(treatment.durationMinutes));
        checkBookable(practitioner.id, start, end);

        Instant now = clock.instant();
        Appointment a = new Appointment();
        a.practitioner = practitioner;
        a.patient = patient;
        a.treatment = treatment;
        a.startsAt = start;
        a.endsAt = end;
        a.createdAt = now;
        a.updatedAt = now;
        translatingOverlaps(() -> {
            appointments.persist(a);
            appointments.flush();
        });
        reminders.schedule(a); // same transaction: the booking and its reminder exist together or not at all
        metrics.appointment("booked");
        return AppointmentView.of(a, config.timeZone());
    }

    @Transactional
    public AppointmentView get(long id) {
        return AppointmentView.of(load(id), config.timeZone());
    }

    /** What the caller may list: reception sees what it asks for; a patient or a practitioner only their own. */
    private AppointmentRepository.Filter restricted(AppointmentRepository.Filter f) {
        if (caller.isReception()) {
            return f;
        }
        Long patient = f.patientId();
        Long practitioner = f.practitionerId();
        if (caller.hasRole(Caller.PATIENT)) {
            Long own = caller.patientId().orElse(-1L); // a patient token without patient_id sees nothing
            if (patient != null && !patient.equals(own)) {
                throw ClinicException.forbidden("You can only list your own appointments");
            }
            patient = own;
        }
        if (caller.hasRole(Caller.PRACTITIONER)) {
            Long own = caller.practitionerId().orElse(-1L);
            if (practitioner != null && !practitioner.equals(own)) {
                throw ClinicException.forbidden("You can only list your own appointments");
            }
            practitioner = own;
        }
        return new AppointmentRepository.Filter(practitioner, patient, f.status(), f.from(), f.to());
    }

    @Transactional
    public PageView<AppointmentView> page(AppointmentRepository.Filter filter, int page, int size) {
        AppointmentRepository.Page result = appointments.page(restricted(filter), page, size);
        List<AppointmentView> items = result.items().stream().map(a -> AppointmentView.of(a, config.timeZone())).toList();
        return new PageView<>(items, page, size, result.total());
    }

    @Transactional
    public AppointmentView reschedule(long id, Long ifMatch, Instant newStart) {
        Appointment a = load(id);
        checkVersion(a, ifMatch);
        requireBooked(a, "rescheduled");
        if (!clock.instant().isBefore(a.startsAt)) {
            throw ClinicException.conflict("ALREADY_STARTED", "An appointment that has started cannot be rescheduled");
        }
        Instant newEnd = newStart.plus(Duration.ofMinutes(a.treatment.durationMinutes));
        checkBookable(a.practitioner.id, newStart, newEnd);

        a.startsAt = newStart;
        a.endsAt = newEnd;
        a.updatedAt = clock.instant();
        translatingOverlaps(appointments::flush);
        reminders.reschedule(a);
        metrics.appointment("rescheduled");
        return AppointmentView.of(a, config.timeZone());
    }

    /** BOOKED -> CANCELLED (before it starts) | COMPLETED | NO_SHOW (once it has started). Nothing leaves those three. */
    @Transactional
    public AppointmentView transition(long id, Long ifMatch, AppointmentStatus to) {
        Appointment a = load(id);
        boolean mayJudgeTheVisit = caller.isReception()
                || caller.practitionerId().filter(p -> p.equals(a.practitioner.id)).isPresent();
        if (to != AppointmentStatus.CANCELLED && !mayJudgeTheVisit) {
            throw ClinicException.forbidden("Only the practitioner or the reception can record how a visit went");
        }
        checkVersion(a, ifMatch);
        if (a.status != AppointmentStatus.BOOKED || to == AppointmentStatus.BOOKED) {
            throw ClinicException.conflict("INVALID_TRANSITION", "An appointment cannot go from " + a.status + " to " + to);
        }
        Instant now = clock.instant();
        if (to == AppointmentStatus.CANCELLED && !now.isBefore(a.startsAt)) {
            throw ClinicException.conflict("ALREADY_STARTED", "An appointment that has started cannot be cancelled");
        }
        if (to != AppointmentStatus.CANCELLED && now.isBefore(a.startsAt)) {
            throw ClinicException.conflict("TOO_EARLY", "The appointment has not started yet");
        }
        a.status = to;
        a.updatedAt = now;
        translatingOverlaps(appointments::flush);
        reminders.cancelPending(a);
        metrics.appointment(to.name().toLowerCase());
        return AppointmentView.of(a, config.timeZone());
    }

    /** An appointment of somebody else is a 404, exactly like one that does not exist: its existence is not revealed (lab 19). */
    private Appointment load(long id) {
        return appointments.findDetailed(id).filter(caller::owns)
                .orElseThrow(() -> ClinicException.notFound("Appointment " + id + " does not exist"));
    }

    private static void checkVersion(Appointment a, Long ifMatch) {
        if (ifMatch == null) {
            throw ClinicException.preconditionRequired();
        }
        if (a.version != ifMatch) {
            throw ClinicException.preconditionFailed("The appointment is at version " + a.version + ", not " + ifMatch);
        }
    }

    private static void requireBooked(Appointment a, String verb) {
        if (a.status != AppointmentStatus.BOOKED) {
            throw ClinicException.conflict("INVALID_TRANSITION", "Only a booked appointment can be " + verb);
        }
    }

    /** The rules of the clinic about WHEN, all in the clinic's zone. Nothing here looks at other appointments. */
    private void checkBookable(long practitionerId, Instant start, Instant end) {
        ZoneId zone = config.timeZone();
        ZonedDateTime local = start.atZone(zone);
        if (local.getSecond() != 0 || local.getNano() != 0 || local.getMinute() % config.slotMinutes() != 0) {
            throw ClinicException.unprocessable("INVALID_START",
                    "Appointments start on a " + config.slotMinutes() + "-minute grid (clinic time)");
        }
        Instant now = clock.instant();
        if (start.isBefore(now)) {
            throw ClinicException.unprocessable("IN_THE_PAST", "The appointment would start in the past");
        }
        if (start.isAfter(now.plus(Duration.ofDays(config.horizonDays())))) {
            throw ClinicException.unprocessable("TOO_FAR", "Appointments can be booked up to " + config.horizonDays() + " days ahead");
        }
        List<Block> blocks = hours.ofPractitionerOnWeekday(practitionerId, local.getDayOfWeek().getValue()).stream()
                .map(h -> new Block(h.startTime, h.endTime)).toList();
        if (!SlotCalculator.fitsInOneBlock(start, end, zone, blocks)) {
            throw ClinicException.unprocessable("OUTSIDE_WORKING_HOURS", "The practitioner does not work during that time");
        }
    }

    /**
     * Runs the writes NOW, so that the verdict of the database arrives HERE, where it can be translated, and not at
     * commit. With {@code IDENTITY} ids the INSERT of a new row happens inside {@code persist()}, not at {@code flush()}.
     */
    private void translatingOverlaps(Runnable writes) {
        try {
            writes.run();
        } catch (PersistenceException e) {
            String constraint = Constraints.violated(e).orElseThrow(() -> e);
            switch (constraint) {
                case Constraints.PRACTITIONER_OVERLAP ->
                    throw ClinicException.conflict("SLOT_TAKEN", "The practitioner is already booked at that time");
                case Constraints.PATIENT_OVERLAP ->
                    throw ClinicException.conflict("PATIENT_BUSY", "The patient already has an appointment at that time");
                default -> throw e;
            }
        }
    }
}
