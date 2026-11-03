package lab.melostudy;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** What an operator would look at: the orders that need a human or a replay. */
@Path("/dead-letters")
@Produces(MediaType.APPLICATION_JSON)
public class DeadLetterResource {

    @Inject
    DeadLetterStore store;

    @GET
    public List<DeadLetter> list() {
        return store.all();
    }
}
