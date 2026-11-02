package lab.melostudy;

/** The product an order refers to does not exist. */
public class UnknownProductException extends RuntimeException {

    public UnknownProductException(long productId) {
        super("Product " + productId + " does not exist");
    }
}
