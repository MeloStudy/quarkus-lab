package lab.melostudy;

import java.net.URI;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/notes")
@Produces(MediaType.APPLICATION_JSON)
public class NotesResource {

    @Inject
    NoteStore store;

    @Inject
    JsonWebToken jwt;

    @GET
    @RolesAllowed({ "reader", "editor", "admin" })
    public List<Note> list() {
        return store.all();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "editor", "admin" })
    public Response create(@Valid NoteRequest request) {
        Note note = store.add(jwt.getName(), request.title());
        return Response.created(URI.create("/api/notes/" + note.id())).entity(note).build();
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed("admin")
    public Response delete(@PathParam("id") long id) {
        return store.remove(id) ? Response.noContent().build() : Response.status(Response.Status.NOT_FOUND).build();
    }
}
