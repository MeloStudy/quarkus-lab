package lab.melostudy;

public class SuspiciousOrderException extends RuntimeException {

    public SuspiciousOrderException(String customerId) {
        super("order of " + customerId + " was flagged by the fraud service");
    }
}
