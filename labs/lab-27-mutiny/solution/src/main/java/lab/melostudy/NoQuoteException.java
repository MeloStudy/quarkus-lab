package lab.melostudy;

/** No source gave a quote for the SKU (all of them failed or were too slow). */
public class NoQuoteException extends RuntimeException {

    public NoQuoteException(String sku) {
        super("No source could quote " + sku);
    }
}
