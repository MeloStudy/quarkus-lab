package lab.melostudy;

/** The rates service does not know the requested currency. */
public class UnknownCurrencyException extends RuntimeException {

    public UnknownCurrencyException() {
        super("The currency is not supported");
    }
}
