package lab.tracker;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/** Test-only endpoint that simulates an unexpected failure (a bug) with a sensitive message. */
@Path("/test-support")
public class BoomResource {

    @GET
    @Path("/boom")
    public String boom() {
        throw new IllegalStateException("secret internal detail: jdbc:postgresql://db-prod:5432/tracker");
    }
}
