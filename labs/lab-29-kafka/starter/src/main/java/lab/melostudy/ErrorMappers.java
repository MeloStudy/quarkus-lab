package lab.melostudy;

import org.apache.kafka.common.KafkaException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.MediaType;
import lab.melostudy.Exceptions.InvalidOrderException;
import lab.melostudy.Exceptions.NoAckException;

/** One error contract. A failure of the producer reaches the endpoint as the exception of the Kafka client. */
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
    public RestResponse<ApiError> noAck(NoAckException e) {
        return answer(504, "NO_ACK", e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> brokerRejected(KafkaException e) {
        return answer(502, "BROKER_REJECTED", "The broker did not take the order: " + e.getClass().getSimpleName());
    }
}
