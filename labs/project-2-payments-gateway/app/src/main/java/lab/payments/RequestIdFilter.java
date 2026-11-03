package lab.payments;

import java.util.UUID;
import java.util.regex.Pattern;

import org.jboss.logging.MDC;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

/** One correlation id per request (lab 25): the caller's when it is safe to use, a new UUID otherwise. */
public class RequestIdFilter {

    static final String HEADER = "X-Request-Id";
    static final String KEY = "requestId";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @ServerRequestFilter(preMatching = true)
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
