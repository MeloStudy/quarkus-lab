package lab.melostudy;

import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Class-level {@code @RolesAllowed}: it covers every method of the resource, present and future. */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
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

    /** A method-level annotation REPLACES the class-level one (it does not add to it). */
    @GET
    @Path("/health-note")
    @RolesAllowed({ "admin", "user" })
    public Map<String, String> healthNote() {
        return Map.of("note", "visible to users too");
    }
}
