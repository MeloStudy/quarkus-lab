package lab.melostudy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** A partner calls with a shared secret in a header. The comparison does not depend on how many characters match. */
@Path("/api/partner/data")
@Produces(MediaType.APPLICATION_JSON)
public class PartnerResource {

    @Inject
    AppConfig config;

    @GET
    public Response data(@HeaderParam("X-Api-Key") String presented) {
        if (presented == null || !sameSecret(presented, config.apiKey())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(Map.of("data", "partner data")).build();
    }

    private static boolean sameSecret(String presented, String expected) {
        return MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }
}
