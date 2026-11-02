package lab.melostudy;

import java.util.UUID;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

/** Adds an X-Request-Id header to every response (echoing the client's, or generating one). */
public class RequestIdFilter {

    static final String HEADER = "X-Request-Id";

    @ServerResponseFilter
    public void addRequestId(ContainerRequestContext request, ContainerResponseContext response) {
        String id = request.getHeaderString(HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        response.getHeaders().putSingle(HEADER, id);
    }
}
