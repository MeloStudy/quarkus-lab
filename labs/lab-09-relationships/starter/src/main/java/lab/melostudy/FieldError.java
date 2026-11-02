package lab.melostudy;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** A single invalid field, e.g. {@code price} / {@code must be greater than 0}. */
@Schema(description = "A single invalid field")
public record FieldError(
        @Schema(description = "Name of the offending field or parameter", example = "price") String field,
        @Schema(description = "What is wrong with it", example = "must be greater than 0") String message) {
}
