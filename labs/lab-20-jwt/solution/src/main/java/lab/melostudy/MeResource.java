package lab.melostudy;

import java.util.Set;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;

/**
 * Shows what the service knows about the caller, read from the verified token.
 * {@code @RequestScoped} is required to inject a single claim as a plain type.
 */
@Path("/api/me")
@RequestScoped
public class MeResource {

    public record Me(String name, String subject, String issuer, Set<String> groups, String email, long expiresAt) {
    }

    @Inject
    JsonWebToken jwt;

    @Inject
    @Claim("email")
    String email;

    @GET
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public Me me() {
        return new Me(jwt.getName(), jwt.getSubject(), jwt.getIssuer(), jwt.getGroups(), email, jwt.getExpirationTime());
    }
}
