package lab.melostudy;

/** A single invalid field, e.g. {@code price} / {@code must be greater than 0}. */
public record FieldError(
        String field,
        String message) {
}
