package lab.melostudy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/info")
public class InfoResource {

    // TODO (Tasks 2 and 3): create a @ConfigMapping (interface) with prefix "app" and inject it here
    // and return a text in the format given in the README.
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String info() {
        return "TODO";
    }
}
