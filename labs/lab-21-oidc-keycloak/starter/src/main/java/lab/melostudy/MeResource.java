package lab.melostudy;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

/** Shows what the service knows about the caller: the name and the roles come from the identity, the rest from the token. */
@Path("/api/me")
public class MeResource {

    public record Me(String name, String subject, String issuer, Set<String> roles, String email, long expiresAt) {
    }

    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken token;

    @GET
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public Me me() {
        return new Me(identity.getPrincipal().getName(), token.getSubject(), token.getIssuer(), identity.getRoles(),
                token.getClaim("email"), token.getExpirationTime());
    }
}
