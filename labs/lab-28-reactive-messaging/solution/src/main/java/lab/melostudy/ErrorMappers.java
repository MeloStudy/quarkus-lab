package lab.melostudy;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.MediaType;
import lab.melostudy.Exceptions.BusyException;
import lab.melostudy.Exceptions.InvalidOrderException;
import lab.melostudy.Exceptions.NoAckException;
import lab.melostudy.Exceptions.OutOfStockException;
import lab.melostudy.Exceptions.WarehouseUnavailableException;

/**
 * One answer for every way a message can be rejected. The failure a stage throws travels back through the channels as a
 * nack and comes out of {@code Emitter.send()} as the same exception, so the mappers can be written as usual.
 */
public class ErrorMappers {

    public record ApiError(String code, String message) {
    }

    private static RestResponse<ApiError> answer(int status, String code, String message) {
        return RestResponse.ResponseBuilder.<ApiError>create(status).entity(new ApiError(code, message)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> invalid(InvalidOrderException e) {
        return answer(422, "INVALID_ORDER", e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> outOfStock(OutOfStockException e) {
        return answer(409, "OUT_OF_STOCK", e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> unavailable(WarehouseUnavailableException e) {
        return answer(502, "WAREHOUSE_UNAVAILABLE", "The warehouse could not be reached; the order was kept for a replay");
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> busy(BusyException e) {
        return RestResponse.ResponseBuilder.<ApiError>create(503).entity(new ApiError("BUSY", e.getMessage())).header("Retry-After", "1")
                .type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> noAck(NoAckException e) {
        return answer(504, "NO_ACK", e.getMessage());
    }
}
