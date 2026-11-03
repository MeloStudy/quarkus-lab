package lab.melostudy;

import org.jboss.logging.MDC;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/** Sends the id of the request being served along with every call to another service. */
public class RequestIdClientFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext context) {
        Object id = MDC.get(RequestIdFilter.KEY);
        if (id != null) {
            context.getHeaders().putSingle(RequestIdFilter.HEADER, id.toString());
        }
    }
}
