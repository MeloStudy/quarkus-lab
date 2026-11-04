package lab.clinic;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import lab.clinic.Repositories.PractitionerRepository;
import lab.clinic.Repositories.TreatmentRepository;
import lab.clinic.Repositories.WorkingHoursRepository;
import lab.clinic.Responses.AvailabilityView;
import lab.clinic.Responses.SlotView;
import lab.clinic.SlotCalculator.Block;
import lab.clinic.SlotCalculator.Interval;

/** "When can this treatment be booked with this practitioner on this day?", answered in the clinic's zone. */
@ApplicationScoped
public class AvailabilityService {

    private final AppointmentRepository appointments;
    private final PractitionerRepository practitioners;
    private final TreatmentRepository treatments;
    private final WorkingHoursRepository hours;
    private final ClinicConfig config;
    private final Clock clock;

    AvailabilityService(AppointmentRepository appointments, PractitionerRepository practitioners,
            TreatmentRepository treatments, WorkingHoursRepository hours, ClinicConfig config, Clock clock) {
        this.appointments = appointments;
        this.practitioners = practitioners;
        this.treatments = treatments;
        this.hours = hours;
        this.config = config;
        this.clock = clock;
    }

    @Transactional
    public AvailabilityView availability(long practitionerId, String dateText, long treatmentId) {
        practitioners.findByIdOptional(practitionerId)
                .orElseThrow(() -> ClinicException.notFound("Practitioner " + practitionerId + " does not exist"));
        Treatment treatment = treatments.findByIdOptional(treatmentId)
                .orElseThrow(() -> ClinicException.notFound("Treatment " + treatmentId + " does not exist"));
        LocalDate date;
        try {
            date = LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            throw ClinicException.badRequest("INVALID_DATE", "'" + dateText + "' is not a date (use yyyy-MM-dd)");
        }

        ZoneId zone = config.timeZone();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<Block> blocks = hours.ofPractitionerOnWeekday(practitionerId, date.getDayOfWeek().getValue()).stream()
                .map(h -> new Block(h.startTime, h.endTime)).toList();
        List<Interval> booked = appointments.booked(practitionerId, dayStart, dayEnd).stream()
                .map(a -> new Interval(a.startsAt, a.endsAt)).toList();

        Instant now = clock.instant();
        List<SlotView> slots = SlotCalculator
                .freeSlots(date, zone, blocks, booked, Duration.ofMinutes(treatment.durationMinutes),
                        Duration.ofMinutes(config.slotMinutes()), now, now.plus(Duration.ofDays(config.horizonDays())))
                .stream().map(s -> new SlotView(s.start(), s.end(), Responses.inZone(s.start(), zone))).toList();
        return new AvailabilityView(date.toString(), zone.getId(), treatment.id, treatment.durationMinutes, slots);
    }
}
