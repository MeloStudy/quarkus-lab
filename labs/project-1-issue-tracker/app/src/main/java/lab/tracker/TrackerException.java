package lab.tracker;

/**
 * Every expected failure of the domain: a status for the client and a stable code to switch on. Unchecked on
 * purpose: it rolls the transaction back (checked exceptions do not, lab 10) and needs no {@code throws} noise.
 */
public class TrackerException extends RuntimeException {

    private final int status;
    private final String code;

    private TrackerException(int status, String code, String message) {
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

    public static TrackerException notFound(String message) {
        return new TrackerException(404, "NOT_FOUND", message);
    }

    /** The request is well-formed but the state of the world does not allow it. */
    public static TrackerException conflict(String code, String message) {
        return new TrackerException(409, code, message);
    }

    /** If-Match does not match the current version, or the version moved while we were working. */
    public static TrackerException preconditionFailed(String message) {
        return new TrackerException(412, "PRECONDITION_FAILED", message);
    }

    /** Changing an issue requires If-Match: a blind overwrite is not allowed. */
    public static TrackerException preconditionRequired() {
        return new TrackerException(428, "PRECONDITION_REQUIRED", "This operation requires an If-Match header");
    }
}
