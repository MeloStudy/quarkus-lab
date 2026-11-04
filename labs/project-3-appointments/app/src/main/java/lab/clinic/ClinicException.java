package lab.clinic;

/**
 * Every expected failure of the domain: a status for the client and a stable code to switch on. Unchecked on
 * purpose: it rolls the transaction back (checked exceptions do not, lab 10) and needs no {@code throws} noise.
 */
public class ClinicException extends RuntimeException {

    private final int status;
    private final String code;

    private ClinicException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public static ClinicException notFound(String message) {
        return new ClinicException(404, "NOT_FOUND", message);
    }

    /** The caller is known but may not do this (a patient completing an appointment, booking for somebody else...). */
    public static ClinicException forbidden(String message) {
        return new ClinicException(403, "FORBIDDEN", message);
    }

    /** The value of a parameter is not usable (an impossible date...). */
    public static ClinicException badRequest(String code, String message) {
        return new ClinicException(400, code, message);
    }

    /** The request is well-formed but breaks a rule of the clinic (the past, the grid, the working hours...). */
    public static ClinicException unprocessable(String code, String message) {
        return new ClinicException(422, code, message);
    }

    /** The request is fine but the state of the world does not allow it (the slot is taken, wrong status...). */
    public static ClinicException conflict(String code, String message) {
        return new ClinicException(409, code, message);
    }

    /** If-Match does not match the current version, or the version moved while we were working. */
    public static ClinicException preconditionFailed(String message) {
        return new ClinicException(412, "PRECONDITION_FAILED", message);
    }

    /** Changing an appointment requires If-Match: a blind overwrite is not allowed. */
    public static ClinicException preconditionRequired() {
        return new ClinicException(428, "PRECONDITION_REQUIRED", "This operation requires an If-Match header");
    }
}
