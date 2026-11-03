package lab.melostudy;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * TODO task 2: only admins may call anything here (one annotation on the class), except /admin/health-note, which
 * users may read too (a different annotation on that method: does it add to the class one or replace it?).
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    private final ReportStore store;

    AdminResource(ReportStore store) {
        this.store = store;
    }

    @GET
    @Path("/stats")
    public Map<String, Integer> stats() {
        return Map.of("reports", store.all().size());
    }

    @GET
    @Path("/health-note")
    public Map<String, String> healthNote() {
        return Map.of("note", "visible to users too");
    }
}
