package lab.payments;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Set;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;

/**
 * Builds signed tokens for the tests, signed with the throw-away private key of {@code src/test/resources} (see the README:
 * it protects nothing and is not in the jar). {@link #forged} uses a key the service has never seen.
 */
final class Tokens {

    static final String ISSUER = "https://payments.example/issuer";
    static final String AUDIENCE = "payments-api";

    private Tokens() {
    }

    /** A valid builder: right issuer, right audience, one hour of life. Replace claims, then sign. */
    static JwtClaimsBuilder valid(String customerId, String... groups) {
        return Jwt.issuer(ISSUER).subject(customerId).upn(customerId).groups(Set.of(groups)).audience(AUDIENCE)
                .expiresIn(Duration.ofHours(1));
    }

    static String customer(String customerId) {
        return valid(customerId, "customer").sign();
    }

    static String finance(String employeeId) {
        return valid(employeeId, "finance").sign();
    }

    static String forged(String customerId, String... groups) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair attacker = generator.generateKeyPair();
            return valid(customerId, groups).sign(attacker.getPrivate());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
