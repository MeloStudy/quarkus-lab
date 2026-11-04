package lab.melostudy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Where is this code running: on a JVM or as a native executable? */
@Path("/info")
@Produces(MediaType.APPLICATION_JSON)
public class InfoResource {

    public record Info(boolean nativeImage, String vm, String javaVersion) {
    }

    @GET
    public Info info() {
        return new Info(System.getProperty("org.graalvm.nativeimage.imagecode") != null, System.getProperty("java.vm.name"),
                System.getProperty("java.version"));
    }
}
