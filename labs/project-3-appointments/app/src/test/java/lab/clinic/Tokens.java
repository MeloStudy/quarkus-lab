package lab.clinic;

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

    static final String ISSUER = "https://clinic.example/issuer";
    static final String AUDIENCE = "clinic-api";

    private Tokens() {
    }

    /** A valid builder: right issuer, right audience, one hour of life. Replace claims, then sign. */
    static JwtClaimsBuilder valid(String subject, String... groups) {
        return Jwt.issuer(ISSUER).subject(subject).upn(subject).groups(Set.of(groups)).audience(AUDIENCE)
                .expiresIn(Duration.ofHours(1));
    }

    static String reception() {
        return valid("front-desk", "reception").sign();
    }

    static String patient(long patientId) {
        return valid("patient-" + patientId, "patient").claim("patient_id", patientId).sign();
    }

    static String practitioner(long practitionerId) {
        return valid("practitioner-" + practitionerId, "practitioner").claim("practitioner_id", practitionerId).sign();
    }

    static String forged(String... groups) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair attacker = generator.generateKeyPair();
            return valid("front-desk", groups).sign(attacker.getPrivate());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
