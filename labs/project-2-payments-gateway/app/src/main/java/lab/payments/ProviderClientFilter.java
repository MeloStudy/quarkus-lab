package lab.payments;

import org.jboss.logging.MDC;

import io.quarkus.arc.Arc;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/**
 * What every call to the provider carries: our credentials, and the id of the request that caused the call (so the
 * provider's support can find it in its own logs). The API key is read when the call is made and is never logged.
 */
public class ProviderClientFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext context) {
        ProviderSettings settings = Arc.container().instance(ProviderSettings.class).get();
        context.getHeaders().putSingle("Authorization", "Bearer " + settings.apiKey());
        Object requestId = MDC.get(RequestIdFilter.KEY);
        if (requestId != null) {
            context.getHeaders().putSingle(RequestIdFilter.HEADER, requestId.toString());
        }
    }
}
