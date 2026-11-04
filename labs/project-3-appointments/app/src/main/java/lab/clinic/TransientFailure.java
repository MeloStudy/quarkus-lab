package lab.clinic;

/** The provider could not take the message NOW (timeout, 5xx, 429, nobody listening): trying again later may work. */
public class TransientFailure extends RuntimeException {

    public TransientFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
