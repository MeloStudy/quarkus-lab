package lab.melostudy;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Typed client for the remote carrier. Its URL and timeouts live under {@code quarkus.rest-client.carrier.*}. */
@RegisterRestClient(configKey = "carrier")
@Path("/quote")
@Produces(MediaType.APPLICATION_JSON)
public interface CarrierClient {

    record CarrierQuote(String city, double price) {
    }

    @GET
    @Path("/{city}")
    CarrierQuote quote(@RestPath String city);

    /**
     * 404 means "this city does not exist" (the caller's mistake, retrying cannot fix it); any other error status means
     * the carrier is in trouble (retrying may help).
     */
    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        int status = response.getStatus();
        if (status == 404) {
            return new UnknownCityException();
        }
        if (status >= 400) {
            return new CarrierUnavailableException("The carrier answered HTTP " + status);
        }
        return null;
    }
}
