package lab.melostudy;

import java.util.UUID;
import java.util.regex.Pattern;

import org.jboss.logging.MDC;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

/**
 * Gives every request one correlation id: the caller's {@code X-Request-Id} when it is safe to use, a new UUID
 * otherwise. The id goes into the logging context (MDC) and comes back in the response.
 */
public class RequestIdFilter {

    static final String HEADER = "X-Request-Id";
    static final String KEY = "requestId";

    /** Letters, digits, dot, underscore and hyphen, 1 to 64 characters: nothing a log search or a dashboard chokes on. */
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @ServerRequestFilter
    public void incoming(ContainerRequestContext request) {
        String id = request.getHeaderString(HEADER);
        if (id == null || !SAFE.matcher(id).matches()) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(KEY, id);
        request.setProperty(KEY, id);
    }

    @ServerResponseFilter
    public void outgoing(ContainerRequestContext request, ContainerResponseContext response) {
        Object id = request.getProperty(KEY);
        if (id != null) {
            response.getHeaders().putSingle(HEADER, id.toString());
        }
    }
}
