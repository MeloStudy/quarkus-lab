package lab.melostudy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greeting")
public class GreetingResource {

    // TODO (Task 1): read the configuration with @ConfigProperty instead of hardcoded values
    //   greeting.message      -> required (already in application.properties)
    //   greeting.name         -> optional (Optional<String>); if missing, use "world"
    //   greeting.punctuation  -> default value "!"
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String greeting() {
        return "Hello, world!";
    }
}
