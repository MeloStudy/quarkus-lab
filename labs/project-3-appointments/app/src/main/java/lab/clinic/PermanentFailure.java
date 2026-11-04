package lab.clinic;

/** The provider refused the message for good (a 4xx: bad address...): sending it again changes nothing. */
public class PermanentFailure extends RuntimeException {

    public PermanentFailure(String message) {
        super(message);
    }
}
