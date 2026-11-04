package lab.clinic;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Everything a client may send, in one place. Bean Validation answers the shape of each field (400); the rules of
 * the clinic (the grid, the working hours, the past...) are checked by the services (422).
 */
public final class Requests {

    private Requests() {
    }

    public record PractitionerRequest(@NotBlank @Size(max = 80) String name) {
    }

    public record TreatmentRequest(@NotBlank @Size(max = 80) String name,
            @Min(value = 15, message = "must be at least 15") @Max(value = 240, message = "must be at most 240") int durationMinutes) {
    }

    public record PatientRequest(@NotBlank @Size(max = 80) String fullName,
            @NotBlank @Email @Size(max = 120) String email) {
    }

    /** A block of local time on one weekday (1 = Monday ... 7 = Sunday). */
    public record HoursBlock(@Min(1) @Max(7) int weekday, @NotNull LocalTime start, @NotNull LocalTime end) {
    }

    /** The whole week replaces the previous one: a PUT never leaves old blocks behind. */
    public record WorkingHoursRequest(@NotNull @Size(max = 30) List<@NotNull @Valid HoursBlock> blocks) {
    }

    /** {@code startsAt} must carry an offset or a Z: a local time without a zone is rejected as malformed. */
    public record BookRequest(@NotNull Long practitionerId, @NotNull Long patientId, @NotNull Long treatmentId,
            @NotNull Instant startsAt) {
    }

    public record RescheduleRequest(@NotNull Instant startsAt) {
    }

    public record TransitionRequest(@NotNull AppointmentStatus to) {
    }
}
