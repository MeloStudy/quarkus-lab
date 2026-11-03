package lab.melostudy;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Calls the notes endpoint as an "other service" would. In this lab the other service is the same application. */
@RegisterRestClient(configKey = "notes")
// TODO task 4: send the token of the caller with every call of this client
@Path("/api/notes")
@Produces(MediaType.APPLICATION_JSON)
public interface NotesClient {

    @GET
    List<Note> list();
}
