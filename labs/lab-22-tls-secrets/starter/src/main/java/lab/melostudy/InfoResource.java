package lab.melostudy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Tells which settings are in use without revealing the secret one. */
@Path("/api/info")
@Produces(MediaType.APPLICATION_JSON)
public class InfoResource {

    @Inject
    AppConfig config;

    @GET
    public Map<String, Object> info() {
        return Map.of("name", config.name(), "apiKeyFingerprint", fingerprint(config.apiKey()));
    }

    /** First 8 hex characters of the SHA-256: enough to tell two keys apart, useless to recover one. */
    static String fingerprint(String secret) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 4);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
