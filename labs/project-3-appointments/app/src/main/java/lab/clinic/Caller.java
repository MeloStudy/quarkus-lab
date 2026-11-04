package lab.clinic;

import java.util.Optional;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Who is calling, as the rules of the clinic need to know it. The roles come from the {@code groups} claim of the
 * token; the link to the clinic's own records comes from two claims: {@code patient_id} and {@code practitioner_id}.
 *
 * <p>A role without its claim (a patient token with no {@code patient_id}) is a token the clinic cannot place: it is
 * treated as having no records at all, so it sees and changes nothing that belongs to somebody.
 */
@ApplicationScoped
public class Caller {

    public static final String RECEPTION = "reception";
    public static final String PRACTITIONER = "practitioner";
    public static final String PATIENT = "patient";

    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken token;

    public boolean hasRole(String role) {
        return identity.hasRole(role);
    }

    public boolean isReception() {
        return identity.hasRole(RECEPTION);
    }

    /** The patient this caller is, if it is one. */
    public Optional<Long> patientId() {
        return identity.hasRole(PATIENT) ? numericClaim("patient_id") : Optional.empty();
    }

    /** The practitioner this caller is, if it is one. */
    public Optional<Long> practitionerId() {
        return identity.hasRole(PRACTITIONER) ? numericClaim("practitioner_id") : Optional.empty();
    }

    /** Does this appointment belong to the caller: as its patient or as its practitioner? Reception owns everything. */
    public boolean owns(Appointment a) {
        return isReception() || patientId().filter(id -> id.equals(a.patient.id)).isPresent()
                || practitionerId().filter(id -> id.equals(a.practitioner.id)).isPresent();
    }

    private Optional<Long> numericClaim(String name) {
        Object value = token.getClaim(name);
        if (value instanceof Number n) {
            return Optional.of(n.longValue());
        }
        if (value instanceof jakarta.json.JsonNumber n) {
            return Optional.of(n.longValue());
        }
        return Optional.empty();
    }
}
