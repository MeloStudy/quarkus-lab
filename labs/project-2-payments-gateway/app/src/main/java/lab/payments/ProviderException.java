package lab.payments;

/**
 * Everything that can go wrong when asking the provider, sorted by the one question that matters to us:
 * <b>can the money have moved?</b>
 * <ul>
 *   <li>{@link Declined}: the provider answered "no". Final, nobody was charged.</li>
 *   <li>{@link Rejected}: the provider refused to look at the request (bad credentials, bad request). Nobody was charged.</li>
 *   <li>{@link Unreachable}: we could not even open a connection. Nothing was sent.</li>
 *   <li>{@link Transient}: it answered 5xx or 429, timed out, dropped the connection or sent garbage. The charge may
 *       or may not have happened.</li>
 *   <li>{@link NotFound}: a lookup found nothing: the provider never saw that key.</li>
 * </ul>
 */
public abstract class ProviderException extends RuntimeException {

    protected ProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class Declined extends ProviderException {
        private final String reason;

        public Declined(String reason) {
            super("The provider declined the payment: " + reason, null);
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }

    public static class Rejected extends ProviderException {
        private final int status;

        public Rejected(int status) {
            super("The provider rejected the request with HTTP " + status, null);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }

    public static class Unreachable extends ProviderException {
        public Unreachable(Throwable cause) {
            super("The provider could not be reached", cause);
        }
    }

    public static class Transient extends ProviderException {
        public Transient(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class NotFound extends ProviderException {
        public NotFound() {
            super("The provider has no record of that key", null);
        }
    }
}
