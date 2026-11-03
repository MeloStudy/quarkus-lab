package lab.melostudy;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Two kinds of rules live here, and they are different tools:
 * <ul>
 *   <li><b>Which roles may call the method</b>: {@code @RolesAllowed}, enforced before the method runs.</li>
 *   <li><b>Which data this caller may see</b>: code that asks the {@link SecurityIdentity}. An annotation cannot
 *       know that report 7 belongs to somebody else.</li>
 * </ul>
 */
@Path("/api/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReportResource {

    private final ReportStore store;
    private final SecurityIdentity identity;

    ReportResource(ReportStore store, SecurityIdentity identity) {
        this.store = store;
        this.identity = identity;
    }

    // TODO task 2: which roles may call each method (see RolesTest and OwnershipTest for who).
    // TODO task 3: a user lists only their own reports; an admin lists all of them.
    @GET
    public List<Report> list() {
        return store.all();
    }

    // TODO task 3: somebody else's report must look exactly like a report that does not exist (and an admin sees any).
    @GET
    @Path("/{id}")
    public Report get(@RestPath long id) {
        return store.find(id).orElseThrow(NotFoundException::new);
    }

    @POST
    public RestResponse<Report> create(ReportRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("title is required");
        }
        // TODO task 3: the owner is the authenticated user (helper me() below), not a constant
        Report created = store.add("nobody", request.title().strip(), request.body());
        return RestResponse.ResponseBuilder.<Report>created(URI.create("/api/reports/" + created.id()))
                .entity(created).build();
    }

    @DELETE
    @Path("/{id}")
    public RestResponse<Void> delete(@RestPath long id) {
        if (!store.remove(id)) {
            throw new NotFoundException();
        }
        return RestResponse.noContent();
    }

    private String me() {
        return identity.getPrincipal().getName();
    }

    private boolean isAdmin() {
        return identity.hasRole("admin");
    }
}
