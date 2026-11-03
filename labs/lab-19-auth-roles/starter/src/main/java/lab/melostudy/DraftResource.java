package lab.melostudy;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * An endpoint somebody wrote in a hurry and FORGOT to protect: no annotation, no path policy covers {@code /api/}.
 * It is here on purpose, to show what the project-wide default does with it.
 */
@Path("/api/drafts")
@Produces(MediaType.APPLICATION_JSON)
public class DraftResource {

    @GET
    public List<String> drafts() {
        return List.of("quarterly numbers (confidential)");
    }
}
