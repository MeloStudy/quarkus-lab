package lab.clinic;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/** Test-only: an endpoint that fails in a way nobody planned, to prove the catch-all reveals nothing. */
@Path("/test-only/boom")
public class BoomResource {

    @GET
    public String boom() {
        throw new IllegalStateException("secret internal detail: jdbc:postgresql://db.internal/clinic");
    }
}
