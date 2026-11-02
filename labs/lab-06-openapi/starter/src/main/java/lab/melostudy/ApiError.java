package lab.melostudy;

import java.util.List;

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
public record ApiError(String code, String message, List<FieldError> errors, String errorId) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null, null);
    }
}
