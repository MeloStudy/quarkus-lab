package lab.melostudy;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException() {
        super("Not enough stock");
    }
}
