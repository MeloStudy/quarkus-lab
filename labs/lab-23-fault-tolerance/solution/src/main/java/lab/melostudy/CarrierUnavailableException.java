package lab.melostudy;

/** The carrier failed (error status, no answer). */
public class CarrierUnavailableException extends RuntimeException {
    public CarrierUnavailableException(String message) {
        super(message);
    }
}
