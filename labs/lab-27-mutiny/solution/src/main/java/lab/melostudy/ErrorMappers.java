package lab.melostudy;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.MediaType;

public class ErrorMappers {

    public record ApiError(String code, String message) {
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> noQuote(NoQuoteException e) {
        return RestResponse.ResponseBuilder.<ApiError>create(503).entity(new ApiError("NO_QUOTE", e.getMessage()))
                .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
