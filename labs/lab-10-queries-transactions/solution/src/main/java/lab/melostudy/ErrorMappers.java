package lab.melostudy;

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
 * Translates every exception into the one {@link ApiError} shape.
 * Mappers are chosen by the most specific exception type, so the catch-all
 * ({@code Throwable}) only handles what nobody else claimed.
 */
public class ErrorMappers {

    private static final Logger LOG = Logger.getLogger(ErrorMappers.class);

    @ServerExceptionMapper
    public RestResponse<ApiError> validation(ConstraintViolationException e) {
        List<FieldError> errors = e.getConstraintViolations().stream()
                .map(v -> new FieldError(fieldName(v), v.getMessage()))
                .sorted(Comparator.comparing(FieldError::field).thenComparing(FieldError::message))
                .toList();
        return json(400, new ApiError("VALIDATION_ERROR", "The request is not valid", errors, null));
    }

    /**
     * Valid JSON with the wrong shape (e.g. a string where a number is expected).
     * Map the input-side exception only: a broader JsonProcessingException mapper would also
     * catch response-serialization bugs and wrongly report them as the client's fault.
     */
    @ServerExceptionMapper
    public RestResponse<ApiError> wrongShape(MismatchedInputException e) {
        return malformedRequest();
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> duplicate(DuplicateProductException e) {
        return json(409, ApiError.of("CONFLICT", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> outOfStock(OutOfStockException e) {
        return json(409, ApiError.of("OUT_OF_STOCK", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> unknownProduct(UnknownProductException e) {
        return json(404, ApiError.of("NOT_FOUND", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> paymentRefused(PaymentRefusedException e) {
        return json(402, ApiError.of("PAYMENT_REFUSED", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> unknownCurrency(UnknownCurrencyException e) {
        return json(400, ApiError.of("UNSUPPORTED_CURRENCY", e.getMessage()));
    }

    /** Our dependency failed: 502 Bad Gateway. The cause stays in the log, never in the body. */
    @ServerExceptionMapper
    public RestResponse<ApiError> ratesUnavailable(RatesUnavailableException e) {
        LOG.warn("Rates service unavailable: " + e.getMessage(), e.getCause());
        return json(502, ApiError.of("UPSTREAM_ERROR", "A service this API depends on is not available"));
    }

    /** Our dependency was too slow: 504 Gateway Timeout. */
    @ServerExceptionMapper
    public RestResponse<ApiError> ratesTimeout(RatesTimeoutException e) {
        LOG.warn(e.getMessage());
        return json(504, ApiError.of("UPSTREAM_TIMEOUT", "A service this API depends on did not answer in time"));
    }

    /** Keeps the status of 404, 405, 406, 415... and only changes the body. */
    @ServerExceptionMapper
    public RestResponse<ApiError> http(WebApplicationException e) {
        if (e.getCause() instanceof JsonProcessingException) {
            // Syntactically broken JSON arrives wrapped in a plain 400 WebApplicationException.
            return malformedRequest();
        }
        int status = e.getResponse().getStatus();
        String code = status == 404 ? "NOT_FOUND" : "HTTP_" + status;
        return json(status, ApiError.of(code, e.getMessage()));
    }

    /** Last line of defense: log everything, reveal nothing but an id. */
    @ServerExceptionMapper
    public RestResponse<ApiError> unexpected(Throwable t) {
        String errorId = UUID.randomUUID().toString();
        LOG.errorf(t, "Unexpected error %s", errorId);
        return json(500, new ApiError("INTERNAL_ERROR", "An unexpected error occurred", null, errorId));
    }

    private static RestResponse<ApiError> malformedRequest() {
        // Deliberately ignores the Jackson message: it describes internals.
        return json(400, ApiError.of("MALFORMED_REQUEST", "The request body is not valid JSON for this operation"));
    }

    private static RestResponse<ApiError> json(int status, ApiError body) {
        return RestResponse.ResponseBuilder.<ApiError>create(status)
                .entity(body)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    /** "create.input.name" -> "name": the client knows JSON names, not Java method internals. */
    private static String fieldName(ConstraintViolation<?> violation) {
        String last = "";
        for (Path.Node node : violation.getPropertyPath()) {
            last = node.getName();
        }
        return last;
    }
}
