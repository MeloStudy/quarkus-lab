package lab.melostudy;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Every call must carry the header {@code x-api-key}. gRPC headers are called <b>metadata</b>. A global interceptor sees every
 * call to every service of the application (the health and reflection services too). The key is required configuration: with
 * no {@code app.api-key} the application does not start.
 */
// TODO task 4: this interceptor is not active yet: make it apply to every call of every service.
@ApplicationScoped
public class ApiKeyInterceptor implements ServerInterceptor {

    static final Metadata.Key<String> API_KEY = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    private final String expected;

    public ApiKeyInterceptor(@ConfigProperty(name = "app.api-key") String expected) {
        this.expected = expected;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        // TODO task 4: a call whose x-api-key header is not the expected key ends with UNAUTHENTICATED and never reaches the service
        //   (call.close(status, new Metadata()) and return an empty listener).
        return next.startCall(call, headers);
    }
}
