package lab.melostudy;

import java.util.Map;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Public on purpose, and says so with {@code @PermitAll}: open is a decision, not a leftover. */
@Path("/public/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {

    @GET
    @PermitAll
    public Map<String, String> status() {
        return Map.of("status", "UP");
    }
}
