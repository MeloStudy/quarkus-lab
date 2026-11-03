package lab.melostudy;

/** The failures of the order desk, grouped in one file: each one means something different to the sender. */
public final class Exceptions {

    private Exceptions() {
    }

    /** The order itself is wrong (quantity, unknown sku). The sender's fault: never retried, never a dead letter. */
    public static class InvalidOrderException extends RuntimeException {
        public InvalidOrderException(String message) {
            super(message);
        }
    }

    /** The warehouse has no stock. A business answer, not a fault: never retried, never a dead letter. */
    public static class OutOfStockException extends RuntimeException {
        public OutOfStockException(String sku) {
            super("No stock of " + sku);
        }
    }

    /** The warehouse could not be reached. It may work a moment later: retried, and a dead letter when it never does. */
    public static class WarehouseUnavailableException extends RuntimeException {
        public WarehouseUnavailableException(String message) {
            super(message);
        }
    }

    /** The channel cannot take more orders right now. */
    public static class BusyException extends RuntimeException {
        public BusyException(String message) {
            super(message);
        }
    }

    /** Nobody acknowledged or rejected the order in time. */
    public static class NoAckException extends RuntimeException {
        public NoAckException(String message) {
            super(message);
        }
    }
}
