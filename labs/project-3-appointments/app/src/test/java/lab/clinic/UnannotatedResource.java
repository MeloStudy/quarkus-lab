package lab.clinic;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/** Test-only: an endpoint nobody annotated, to prove that the default is closed (lab 19). */
@Path("/test-only/forgotten")
public class UnannotatedResource {

    @GET
    public String forgotten() {
        return "should never be reachable";
    }
}
