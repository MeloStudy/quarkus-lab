package lab.melostudy;

import java.util.Set;
import java.util.TreeSet;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** "Who am I?": any authenticated user, whatever roles they have (even none). */
@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {

    public record Me(String name, Set<String> roles) {
    }

    private final SecurityIdentity identity;

    MeResource(SecurityIdentity identity) {
        this.identity = identity;
    }

    @GET
    @Authenticated
    public Me me() {
        return new Me(identity.getPrincipal().getName(), new TreeSet<>(identity.getRoles()));
    }
}
