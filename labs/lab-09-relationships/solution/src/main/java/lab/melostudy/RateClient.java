package lab.melostudy;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Typed client for the remote exchange-rate service. Its URL and timeouts live in configuration
 * under {@code quarkus.rest-client.rates.*} (the {@code configKey}), never in code.
 */
@RegisterRestClient(configKey = "rates")
@Path("/rates")
@Produces(MediaType.APPLICATION_JSON)
public interface RateClient {

    @GET
    @Path("/{currency}")
    Rate get(@RestPath String currency);

    /**
     * Turns the remote's HTTP errors into our own exceptions. Without this, any 4xx/5xx of the
     * remote service surfaces as a WebApplicationException that carries the REMOTE status code.
     */
    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        int status = response.getStatus();
        if (status == 404) {
            return new UnknownCurrencyException();
        }
        if (status >= 400) {
            return new RatesUnavailableException("The rates service answered HTTP " + status, null);
        }
        return null;
    }
}
