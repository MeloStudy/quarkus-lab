package lab.clinic;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import lab.clinic.Requests.BookRequest;
import lab.clinic.Requests.RescheduleRequest;
import lab.clinic.Requests.TransitionRequest;
import lab.clinic.Responses.AppointmentView;
import lab.clinic.Responses.PageView;

/**
 * Appointments. The ETag of an appointment is its version: GET, POST and the actions give it, and rescheduling or
 * changing the status must send it back in {@code If-Match} (lab 15).
 */
@Path("/api/appointments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AppointmentResource {

    private final BookingService booking;
    private final ClinicConfig config;

    AppointmentResource(BookingService booking, ClinicConfig config) {
        this.booking = booking;
        this.config = config;
    }

    @POST
    @RolesAllowed({ Caller.RECEPTION, Caller.PATIENT })
    public Response book(@Valid @NotNull BookRequest request) {
        AppointmentView created = booking.book(request);
        return Response.created(URI.create("/api/appointments/" + created.id())).tag(tagOf(created)).entity(created).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ Caller.RECEPTION, Caller.PRACTITIONER, Caller.PATIENT })
    public Response get(@RestPath long id) {
        AppointmentView a = booking.get(id);
        return Response.ok(a).tag(tagOf(a)).build();
    }

    /**
     * Filters arrive as validated TEXT: a client mistake is a 400 that names the parameter, not a 404 from the
     * conversion. {@code from} and {@code to} are days of the clinic, both included.
     */
    @GET
    @RolesAllowed({ Caller.RECEPTION, Caller.PRACTITIONER, Caller.PATIENT })
    public PageView<AppointmentView> list(
            @RestQuery @Pattern(regexp = "[0-9]{1,18}") String practitionerId,
            @RestQuery @Pattern(regexp = "[0-9]{1,18}") String patientId,
            @RestQuery @Pattern(regexp = "BOOKED|CANCELLED|COMPLETED|NO_SHOW") String status,
            @RestQuery @Pattern(regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}") String from,
            @RestQuery @Pattern(regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}") String to,
            @RestQuery @DefaultValue("0") @Pattern(regexp = "[0-9]{1,6}") String page,
            @RestQuery @DefaultValue("20") @Pattern(regexp = "[1-9][0-9]?|100") String size) {
        AppointmentRepository.Filter filter = new AppointmentRepository.Filter(
                practitionerId == null ? null : Long.parseLong(practitionerId),
                patientId == null ? null : Long.parseLong(patientId),
                status == null ? null : AppointmentStatus.valueOf(status),
                from == null ? null : parseDay(from).atStartOfDay(config.timeZone()).toInstant(),
                to == null ? null : parseDay(to).plusDays(1).atStartOfDay(config.timeZone()).toInstant());
        return booking.page(filter, Integer.parseInt(page), Integer.parseInt(size));
    }

    @POST
    @Path("/{id}/reschedule")
    @RolesAllowed({ Caller.RECEPTION, Caller.PATIENT })
    public Response reschedule(@RestPath long id, @RestHeader("If-Match") String ifMatch,
            @Valid @NotNull RescheduleRequest request) {
        AppointmentView a = booking.reschedule(id, versionOf(ifMatch), request.startsAt());
        return Response.ok(a).tag(tagOf(a)).build();
    }

    @POST
    @Path("/{id}/status")
    @RolesAllowed({ Caller.RECEPTION, Caller.PRACTITIONER, Caller.PATIENT })
    public Response transition(@RestPath long id, @RestHeader("If-Match") String ifMatch,
            @Valid @NotNull TransitionRequest request) {
        AppointmentView a = booking.transition(id, versionOf(ifMatch), request.to());
        return Response.ok(a).tag(tagOf(a)).build();
    }

    private static LocalDate parseDay(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw ClinicException.badRequest("INVALID_DATE", "'" + text + "' is not a date (use yyyy-MM-dd)");
        }
    }

    private static EntityTag tagOf(AppointmentView a) {
        return new EntityTag(Long.toString(a.version()));
    }

    /**
     * {@code If-Match: "3"} -> 3. No header -> null (the service answers 428). A value that is not one of our ETags
     * (garbage, {@code *}, a weak tag) can never match: -1 is no version, so the service answers 412.
     */
    private static Long versionOf(String ifMatch) {
        if (ifMatch == null) {
            return null;
        }
        String value = ifMatch.strip();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
