package lab.payments;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/** Test-only endpoints (they exist while the tests run): one nobody annotated, and one that fails in a way nobody planned. */
@Path("/api/_probe")
public class TestProbe {

    @GET
    @Path("closed")
    public String notAnnotated() {
        return "this should never be reachable";
    }

    @GET
    @Path("boom")
    @RolesAllowed("customer")
    public String boom() {
        throw new IllegalStateException("secret internal detail: jdbc:postgresql://db.internal:5432/payments");
    }
}
