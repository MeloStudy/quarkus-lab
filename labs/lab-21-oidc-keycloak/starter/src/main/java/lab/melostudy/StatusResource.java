package lab.melostudy;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/public/status")
public class StatusResource {

    @GET
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String status() {
        return "up";
    }
}
