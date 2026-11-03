package lab.tracker;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The one error shape of the API (lab 05).
 *
 * @param code    stable identifier clients switch on
 * @param message human-readable summary
 * @param errors  per-field problems (validation only)
 * @param errorId correlation id of an unexpected failure (also in the log)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, List<FieldError> errors, String errorId) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null, null);
    }
}
