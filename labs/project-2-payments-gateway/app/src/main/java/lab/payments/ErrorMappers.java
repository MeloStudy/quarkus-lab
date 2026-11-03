package lab.payments;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

/**
 * Every failure leaves the API as an {@link ApiError}. The framework picks the mapper with the most specific exception
 * type, so the {@code Throwable} one only receives what nobody else claimed. Validation messages never repeat the value
 * that was sent: a customer who pastes a card number in the wrong field must not see it echoed, or find it in a log.
 */
public class ErrorMappers {

    private static final Logger LOG = Logger.getLogger(ErrorMappers.class);

    @ServerExceptionMapper
    public RestResponse<ApiError> domain(PaymentException e) {
        RestResponse.ResponseBuilder<ApiError> builder = RestResponse.ResponseBuilder.<ApiError>create(e.status())
                .entity(e.paymentId() == null ? ApiError.of(e.code(), e.getMessage())
                        : ApiError.forPayment(e.code(), e.getMessage(), e.paymentId()))
                .type(MediaType.APPLICATION_JSON_TYPE);
        if (e.retryAfterSeconds() != null) {
            builder.header("Retry-After", e.retryAfterSeconds());
        }
        return builder.build();
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> validation(ConstraintViolationException e) {
        List<ApiError.FieldError> errors = e.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldError(fieldName(v), v.getMessage()))
                .sorted(Comparator.comparing(ApiError.FieldError::field).thenComparing(ApiError.FieldError::message))
                .toList();
        return json(400, ApiError.invalid(errors));
    }

    /** Valid JSON of the wrong shape: a string for a number, an unknown field... */
    @ServerExceptionMapper
    public RestResponse<ApiError> wrongShape(MismatchedInputException e) {
        return malformed();
    }

    /** Keeps the status of 404, 405, 415... and only replaces the body. */
    @ServerExceptionMapper
    public RestResponse<ApiError> http(WebApplicationException e) {
        if (e.getCause() instanceof JsonProcessingException) {
            return malformed();
        }
        int status = e.getResponse().getStatus();
        return json(status, ApiError.of(status == 404 ? "NOT_FOUND" : "HTTP_" + status, e.getMessage()));
    }

    /** Last line of defense: log everything, reveal nothing but an id. */
    @ServerExceptionMapper
    public RestResponse<ApiError> unexpected(Throwable t) {
        String errorId = UUID.randomUUID().toString();
        LOG.errorf(t, "Unexpected error %s", errorId);
        return json(500, ApiError.of("INTERNAL_ERROR", "An unexpected error occurred (error " + errorId + ")"));
    }

    private static RestResponse<ApiError> malformed() {
        return json(400, ApiError.of("MALFORMED_REQUEST", "The request body is not valid for this operation"));
    }

    private static RestResponse<ApiError> json(int status, ApiError body) {
        return RestResponse.ResponseBuilder.<ApiError>create(status).entity(body).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    /** "create.request.cardToken" -> "cardToken": the client knows JSON names, not Java internals. */
    private static String fieldName(ConstraintViolation<?> violation) {
        String last = "";
        for (Path.Node node : violation.getPropertyPath()) {
            String name = node.getName();
            if (name != null && !name.startsWith("<")) {
                last = name;
            }
        }
        return last;
    }
}
