package lab.melostudy;

import java.net.InetAddress;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** What the application sees of the machine it runs on: inside a container it is the limits of the container. */
@Path("/info")
@Produces(MediaType.APPLICATION_JSON)
public class InfoResource {

    public record Info(String message, String javaVersion, long maxHeapMb, int cpus, String user, String host) {
    }

    @ConfigProperty(name = "greeting.message")
    String message;

    @GET
    public Info info() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        return new Info(message, System.getProperty("java.version"), runtime.maxMemory() / (1024 * 1024), runtime.availableProcessors(),
                System.getProperty("user.name"), InetAddress.getLocalHost().getHostName());
    }
}
