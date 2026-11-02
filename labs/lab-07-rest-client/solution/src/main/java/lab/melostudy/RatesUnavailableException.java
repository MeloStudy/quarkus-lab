package lab.melostudy;

/** The rates service failed, is unreachable, or sent something unusable (we answer 502). */
public class RatesUnavailableException extends RuntimeException {

    public RatesUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
