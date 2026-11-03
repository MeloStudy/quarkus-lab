package lab.melostudy;

/** We cannot answer at all: the carrier is failing and we have nothing to offer instead. */
public class ShippingUnavailableException extends RuntimeException {
    public ShippingUnavailableException(String message) {
        super(message);
    }
}
