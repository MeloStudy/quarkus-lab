package lab.melostudy;

import java.net.InetAddress;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** What the configuration of the pod looks like from inside. The token is never returned, only its length. */
@Path("/info")
@Produces(MediaType.APPLICATION_JSON)
public class InfoResource {

    public record Info(String message, int tokenLength, long maxHeapMb, String host) {
    }

    @ConfigProperty(name = "greeting.message")
    String message;

    @ConfigProperty(name = "api.token")
    String token;

    @GET
    public Info info() throws Exception {
        return new Info(message, token.length(), Runtime.getRuntime().maxMemory() / (1024 * 1024), InetAddress.getLocalHost().getHostName());
    }
}
