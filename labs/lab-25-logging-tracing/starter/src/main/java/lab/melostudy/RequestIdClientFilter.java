package lab.melostudy;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/** Sends the id of the request being served along with every call to another service. */
public class RequestIdClientFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext context) {
        // TODO Task 3: read the id of the request being served (RequestIdFilter.KEY in the MDC) and send it in the
        //   RequestIdFilter.HEADER of the outgoing call. This class also has to be registered on the REST client.
    }
}
