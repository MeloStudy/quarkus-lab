package lab.shop;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

/** The application's own endpoint. It does not mention the beans of the library: the library works behind the scenes. */
@Path("/orders")
public class OrderResource {

    @Inject
    Checkout checkout;

    @GET
    @Path("/checkout")
    @Produces("text/plain")
    public String checkout(@QueryParam("customer") String customer) {
        return checkout.checkout(customer);
    }
}
