package lab.melostudy;

/** The upstream does not know this currency. */
public class UnknownCurrencyException extends RuntimeException {

    public UnknownCurrencyException(String currency) {
        super("Unknown currency: " + currency);
    }
}
