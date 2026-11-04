package lab.clinic;

import java.time.Instant;
import java.util.List;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** What the reception sees of the reminders: which ones are stuck, and a way to try a failed one again. */
@Path("/api/reminders")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Caller.RECEPTION)
public class ReminderResource {

    public record ReminderView(long id, long appointmentId, ReminderStatus status, int attempts, Instant dueAt,
            Instant nextAttemptAt, Instant sentAt, String lastError) {
        static ReminderView of(Reminder r) {
            return new ReminderView(r.id, r.appointment.id, r.status, r.attempts, r.dueAt, r.nextAttemptAt, r.sentAt, r.lastError);
        }
    }

    private final ReminderStore store;

    ReminderResource(ReminderStore store) {
        this.store = store;
    }

    @GET
    public List<ReminderView> list(@RestQuery @Pattern(regexp = "PENDING|SENT|FAILED|CANCELLED") String status) {
        return store.list(status == null ? null : ReminderStatus.valueOf(status)).stream().map(ReminderView::of).toList();
    }

    @POST
    @Path("/{id}/retry")
    public ReminderView retry(@RestPath long id) {
        return ReminderView.of(store.retry(id));
    }
}
