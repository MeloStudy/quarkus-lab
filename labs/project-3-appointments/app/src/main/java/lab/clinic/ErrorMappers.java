package lab.clinic;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

/**
 * Every failure leaves the API as an {@link ApiError}. The framework picks the mapper with the most specific
 * exception type, so the {@code Throwable} one only receives what nobody else claimed.
 */
public class ErrorMappers {

    private static final Logger LOG = Logger.getLogger(ErrorMappers.class);

    @Inject
    ClinicMetrics metrics;

    @ServerExceptionMapper
    public RestResponse<ApiError> domain(ClinicException e) {
        if (e.status() == 409 || e.status() == 422) {
            metrics.rejected(e.code()); // the codes are a short fixed list: safe as a tag
        }
        return json(e.status(), ApiError.of(e.code(), e.getMessage()));
    }

    /** Two writers read the same version and both tried to write: the database let only one UPDATE through. */
    @ServerExceptionMapper
    public RestResponse<ApiError> lostRace(OptimisticLockException e) {
        return json(412, ApiError.of("PRECONDITION_FAILED", "The appointment changed since you read it; reload it and try again"));
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> validation(ConstraintViolationException e) {
        List<ApiError.FieldError> errors = e.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldError(fieldName(v), v.getMessage()))
                .sorted(Comparator.comparing(ApiError.FieldError::field).thenComparing(ApiError.FieldError::message))
                .toList();
        return json(400, new ApiError("VALIDATION_ERROR", "The request is not valid", errors, null));
    }

    /** Valid JSON of the wrong shape: a string for a number, an unknown enum value, a date with no offset... */
    @ServerExceptionMapper
    public RestResponse<ApiError> wrongShape(MismatchedInputException e) {
        return malformed();
    }

    /** Keeps the status of 404, 405, 415... and only replaces the body. */
    @ServerExceptionMapper
    public RestResponse<ApiError> http(WebApplicationException e) {
        if (e.getCause() instanceof JsonProcessingException) {
            return malformed(); // syntactically broken JSON arrives as a plain 400 wrapping the Jackson exception
        }
        int status = e.getResponse().getStatus();
        return json(status, ApiError.of(status == 404 ? "NOT_FOUND" : "HTTP_" + status, e.getMessage()));
    }

    /** Last line of defense: log everything, reveal nothing but an id. */
    @ServerExceptionMapper
    public RestResponse<ApiError> unexpected(Throwable t) {
        String errorId = UUID.randomUUID().toString();
        LOG.errorf(t, "Unexpected error %s", errorId);
        return json(500, new ApiError("INTERNAL_ERROR", "An unexpected error occurred", null, errorId));
    }

    private static RestResponse<ApiError> malformed() {
        // Deliberately ignores the Jackson message: it describes internals.
        return json(400, ApiError.of("MALFORMED_REQUEST", "The request body is not valid for this operation"));
    }

    private static RestResponse<ApiError> json(int status, ApiError body) {
        return RestResponse.ResponseBuilder.<ApiError>create(status).entity(body).type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    /** "book.request.startsAt" -> "startsAt": the client knows JSON names, not Java internals. */
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
