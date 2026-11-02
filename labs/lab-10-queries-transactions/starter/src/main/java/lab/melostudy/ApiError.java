package lab.melostudy;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The one error shape every failure of this API must use.
 *
 * @param code    stable, machine-readable identifier (clients switch on this)
 * @param message human-readable summary
 * @param errors  per-field problems (validation only)
 * @param errorId correlation id for unexpected failures (also written to the log)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "The error body every failure of this API uses")
public record ApiError(
        @Schema(description = "Stable, machine-readable error identifier", example = "VALIDATION_ERROR") String code,
        @Schema(description = "Human-readable summary", example = "The request is not valid") String message,
        @Schema(description = "Per-field problems (validation errors only)") List<FieldError> errors,
        @Schema(description = "Correlation id of an unexpected failure; also written to the server log") String errorId) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null, null);
    }
}
