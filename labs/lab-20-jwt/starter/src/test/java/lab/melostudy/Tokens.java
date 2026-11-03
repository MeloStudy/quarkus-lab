package lab.melostudy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Set;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;

/**
 * Builds signed tokens for the tests. Normal tokens are signed with the test private key
 * ({@code smallrye.jwt.sign.key.location}); {@link #forged} uses a key the service has never seen.
 */
public final class Tokens {

    public static final String ISSUER = "https://lab.example/issuer";
    public static final String AUDIENCE = "notes-api";

    private Tokens() {
    }

    /** A valid builder: right issuer, right audience, one hour of life. Add or replace claims, then sign. */
    public static JwtClaimsBuilder valid(String upn, String... groups) {
        return Jwt.issuer(ISSUER)
                .upn(upn)
                .subject("sub-" + upn)
                .groups(Set.of(groups))
                .audience(AUDIENCE)
                .expiresIn(Duration.ofHours(1));
    }

    public static String signed(String upn, String... groups) {
        return valid(upn, groups).sign();
    }

    /** Same claims as a valid token, signed with a freshly generated key. */
    public static String forged(String upn, String... groups) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair attacker = generator.generateKeyPair();
            return valid(upn, groups).sign(attacker.getPrivate());
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
