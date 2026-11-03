package lab.melostudy;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

/** A service that builds its answer by calling another service on behalf of the caller. */
@Path("/api/summary")
@Produces(MediaType.APPLICATION_JSON)
public class SummaryResource {

    public record Summary(int count, List<String> titles) {
    }

    @Inject
    @RestClient
    NotesClient notes;

    @GET
    @RolesAllowed({ "reader", "editor", "admin" })
    public Summary summary() {
        List<Note> all = notes.list();
        return new Summary(all.size(), all.stream().map(Note::title).toList());
    }
}
