package lab.melostudy;

import java.util.Set;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.security.Authenticated;

/** Shows what the service knows about the caller, read from the verified token. */
@Path("/api/me")
@RequestScoped
public class MeResource {

    public record Me(String name, String subject, String issuer, Set<String> groups, String email, long expiresAt) {
    }

    // TODO task 2: inject the JsonWebToken and the "email" claim, and fill the response from them.

    @GET
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public Me me() {
        return new Me(null, null, null, Set.of(), null, 0);
    }
}
