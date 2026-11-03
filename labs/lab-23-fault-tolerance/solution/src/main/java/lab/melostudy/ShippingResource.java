package lab.melostudy;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/shipping")
@Produces(MediaType.APPLICATION_JSON)
public class ShippingResource {

    @Inject
    ShippingService shipping;

    @GET
    @Path("/{city}")
    public Quote quote(@PathParam("city") String city) {
        return shipping.quote(city);
    }
}
