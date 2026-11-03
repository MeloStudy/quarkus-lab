package lab.melostudy;

/** The failures of the shipping service. */
public final class Exceptions {

    private Exceptions() {
    }

    /** The client posted something that must not even be published. */
    public static class InvalidOrderException extends RuntimeException {
        public InvalidOrderException(String message) {
            super(message);
        }
    }

    /** An event reached the consumer with a sku nobody can ship: a "poison" message. */
    public static class UnknownSkuException extends RuntimeException {
        public UnknownSkuException(String sku) {
            super("Unknown sku " + sku);
        }
    }

    /** The broker neither accepted nor rejected the record in time. */
    public static class NoAckException extends RuntimeException {
        public NoAckException(String message) {
            super(message);
        }
    }
}
