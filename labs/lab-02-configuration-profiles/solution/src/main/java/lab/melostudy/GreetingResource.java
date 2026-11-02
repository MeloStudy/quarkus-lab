package lab.melostudy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@Path("/greeting")
public class GreetingResource {

    @ConfigProperty(name = "greeting.message")
    String message;

    @ConfigProperty(name = "greeting.name")
    Optional<String> name;

    @ConfigProperty(name = "greeting.punctuation", defaultValue = "!")
    String punctuation;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String greeting() {
        return message + ", " + name.orElse("world") + punctuation;
    }
}
