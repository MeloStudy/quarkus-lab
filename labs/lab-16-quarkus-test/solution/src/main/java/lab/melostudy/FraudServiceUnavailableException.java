package lab.melostudy;

public class FraudServiceUnavailableException extends RuntimeException {

    public FraudServiceUnavailableException() {
        super("the fraud service is not reachable");
    }
}
