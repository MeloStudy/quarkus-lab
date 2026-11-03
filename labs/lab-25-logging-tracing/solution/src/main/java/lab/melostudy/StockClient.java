package lab.melostudy;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RegisterRestClient(configKey = "stock")
@RegisterProvider(RequestIdClientFilter.class)
@Path("/reserve")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface StockClient {

    @POST
    Reservation reserve(ReserveRequest request);

    @ClientExceptionMapper
    static RuntimeException map(Response response) {
        if (response.getStatus() == 409) {
            return new OutOfStockException();
        }
        return new StockUnavailableException(response.getStatus());
    }
}
