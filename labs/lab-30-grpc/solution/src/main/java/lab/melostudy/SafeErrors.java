package lab.melostudy;

import org.jboss.logging.Logger;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.ServerCall;
import io.quarkus.grpc.ExceptionHandler;
import io.quarkus.grpc.ExceptionHandlerProvider;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * What the client is told when the server fails. By default an exception that is not a gRPC status reaches the client as
 * {@code UNKNOWN} <b>with its message</b> (a host name, a password in a connection string...). Here a failure that nobody
 * meant to report becomes {@code INTERNAL} with a fixed text, and the real cause stays in the log. A status that the code
 * chose on purpose ({@code NOT_FOUND}, {@code INVALID_ARGUMENT}...) goes through untouched.
 */
@ApplicationScoped
public class SafeErrors implements ExceptionHandlerProvider {

    private static final Logger LOG = Logger.getLogger(SafeErrors.class);

    @Override
    public <ReqT, RespT> ExceptionHandler<ReqT, RespT> createHandler(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> call, Metadata metadata) {
        return new ExceptionHandler<>(listener, call, metadata) {
            @Override
            protected void handleException(Throwable t, ServerCall<ReqT, RespT> serverCall, Metadata trailersIfAny) {
                StatusRuntimeException status = (StatusRuntimeException) ExceptionHandlerProvider.toStatusException(transform(t), true);
                Metadata trailers = status.getTrailers() != null ? status.getTrailers() : trailersIfAny;
                serverCall.close(status.getStatus(), trailers);
            }
        };
    }

    @Override
    public Throwable transform(Throwable t) {
        if (t instanceof StatusRuntimeException || t instanceof StatusException) {
            return t;
        }
        LOG.error("Unexpected failure in a gRPC call", t);
        return Status.INTERNAL.withDescription("Internal error").asRuntimeException();
    }
}
