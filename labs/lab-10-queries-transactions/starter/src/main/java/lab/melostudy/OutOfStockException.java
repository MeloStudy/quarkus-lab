package lab.melostudy;

/** Business rule violation: there are fewer units in stock than requested. Unchecked, so it rolls back. */
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(long productId, int requested, int available) {
        super("Product " + productId + " has " + available + " units in stock, " + requested + " requested");
    }
}
