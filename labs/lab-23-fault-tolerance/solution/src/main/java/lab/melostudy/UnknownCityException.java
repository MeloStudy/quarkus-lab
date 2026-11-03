package lab.melostudy;

/** The carrier does not serve this city. A mistake of the caller, not a failure of the carrier. */
public class UnknownCityException extends RuntimeException {
    public UnknownCityException() {
        super("Unknown city");
    }
}
