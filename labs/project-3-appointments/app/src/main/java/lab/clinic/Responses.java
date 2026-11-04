package lab.clinic;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Everything the API returns. Instants travel as UTC ("Z"); {@code local} repeats them in the clinic's zone WITH the offset. */
public final class Responses {

    private Responses() {
    }

    /** "2026-03-29T03:15:00+02:00": the instant seen from the clinic. The offset is what makes it unambiguous. */
    static String inZone(Instant instant, ZoneId zone) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(zone));
    }

    public record PractitionerView(long id, String name) {
        static PractitionerView of(Practitioner p) {
            return new PractitionerView(p.id, p.name);
        }
    }

    public record TreatmentView(long id, String name, int durationMinutes) {
        static TreatmentView of(Treatment t) {
            return new TreatmentView(t.id, t.name, t.durationMinutes);
        }
    }

    public record PatientView(long id, String fullName, String email) {
        static PatientView of(Patient p) {
            return new PatientView(p.id, p.fullName, p.email);
        }
    }

    public record HoursView(int weekday, LocalTime start, LocalTime end) {
        static HoursView of(WorkingHours h) {
            return new HoursView(h.weekday, h.startTime, h.endTime);
        }
    }

    public record AppointmentView(long id, long practitionerId, String practitionerName, long patientId,
            String patientName, long treatmentId, String treatmentName, Instant startsAt, Instant endsAt, String local,
            AppointmentStatus status, long version) {
        static AppointmentView of(Appointment a, ZoneId zone) {
            return new AppointmentView(a.id, a.practitioner.id, a.practitioner.name, a.patient.id, a.patient.fullName,
                    a.treatment.id, a.treatment.name, a.startsAt, a.endsAt, inZone(a.startsAt, zone), a.status, a.version);
        }
    }

    public record PageView<T>(List<T> items, int page, int size, long total) {
    }

    public record SlotView(Instant startsAt, Instant endsAt, String local) {
    }

    public record AvailabilityView(String date, String timeZone, long treatmentId, int durationMinutes,
            List<SlotView> slots) {
    }
}
