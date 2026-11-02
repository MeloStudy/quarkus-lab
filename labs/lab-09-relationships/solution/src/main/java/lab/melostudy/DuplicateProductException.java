package lab.melostudy;

/** Business rule violation: product names are unique (case-insensitive). */
public class DuplicateProductException extends RuntimeException {

    public DuplicateProductException(String name) {
        super("A product named '" + name + "' already exists");
    }
}
