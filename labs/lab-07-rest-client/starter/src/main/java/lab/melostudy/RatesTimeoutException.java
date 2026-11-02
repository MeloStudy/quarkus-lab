package lab.melostudy;

/** The rates service did not answer in time (we answer 504). */
public class RatesTimeoutException extends RuntimeException {

    public RatesTimeoutException(Throwable cause) {
        super("The rates service did not answer in time", cause);
    }
}
