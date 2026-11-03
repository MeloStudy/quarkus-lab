package lab.payments;

/** A failure the API reports on purpose: a status, a stable {@code code}, and (sometimes) when to come back. */
public class PaymentException extends RuntimeException {

    private final int status;
    private final String code;
    private final String paymentId;
    private final Integer retryAfterSeconds;

    public PaymentException(int status, String code, String message) {
        this(status, code, message, null, null);
    }

    public PaymentException(int status, String code, String message, String paymentId, Integer retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.paymentId = paymentId;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String paymentId() {
        return paymentId;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
