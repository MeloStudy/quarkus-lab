package lab.melostudy;

public class StockUnavailableException extends RuntimeException {
    public StockUnavailableException(int status) {
        super("The stock service answered " + status);
    }
}
