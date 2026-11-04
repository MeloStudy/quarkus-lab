package lab.clinic;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

/** Which database constraint refused a statement. Services translate the ones they expect into domain errors. */
final class Constraints {

    static final String PRACTITIONER_OVERLAP = "appointment_no_overlap_practitioner";
    static final String PATIENT_OVERLAP = "appointment_no_overlap_patient";
    static final String EMAIL_TAKEN = "patient_email_lower_uq";
    static final String HOURS_OVERLAP = "working_hours_no_overlap";
    /** PostgreSQL names an unnamed UNIQUE constraint {table}_{column}_key. */
    static final String TREATMENT_NAME_TAKEN = "treatment_name_key";

    private Constraints() {
    }

    /**
     * The name of the violated constraint, looking through the wrappers of JPA and Hibernate. Hibernate fills the name
     * for some violations only; the PostgreSQL driver always carries it in the error message of the server.
     */
    static Optional<String> violated(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
                return Optional.of(cve.getConstraintName());
            }
            if (t instanceof PSQLException pg) {
                ServerErrorMessage server = pg.getServerErrorMessage();
                if (server != null && server.getConstraint() != null) {
                    return Optional.of(server.getConstraint());
                }
            }
        }
        return Optional.empty();
    }
}
