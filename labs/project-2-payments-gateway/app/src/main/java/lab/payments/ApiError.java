package lab.payments;

import java.util.List;

import org.jboss.logging.MDC;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Every failure leaves the API in this shape. {@code requestId} is the correlation id of the request (it is also the
 * {@code X-Request-Id} header): the customer quotes it, support finds the log lines and the trace with it.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, List<FieldError> errors, String paymentId, String requestId) {

    public record FieldError(String field, String message) {
    }

    static ApiError of(String code, String message) {
        return new ApiError(code, message, null, null, currentRequestId());
    }

    static ApiError invalid(List<FieldError> errors) {
        return new ApiError("VALIDATION_ERROR", "The request is not valid", errors, null, currentRequestId());
    }

    static ApiError forPayment(String code, String message, String paymentId) {
        return new ApiError(code, message, null, paymentId, currentRequestId());
    }

    private static String currentRequestId() {
        Object id = MDC.get(RequestIdFilter.KEY);
        return id == null ? null : id.toString();
    }
}
