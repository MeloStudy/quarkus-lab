package lab.melostudy;

public class InventoryUnavailableException extends RuntimeException {
    public InventoryUnavailableException() {
        super("The inventory service is not available");
    }
}
