package lab.melostudy;

import java.net.URI;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** REST API for orders, mounted at /orders. */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    private final OrderService orders;

    OrderResource(OrderService orders) {
        this.orders = orders;
    }

    @POST
    public RestResponse<CustomerOrder> place(@Valid @NotNull OrderInput input) throws PaymentRefusedException {
        CustomerOrder order = orders.place(input.productId(), input.quantity());
        return RestResponse.ResponseBuilder.<CustomerOrder>created(URI.create("/orders/" + order.id)).entity(order).build();
    }

    @GET
    @Path("/{id}")
    public CustomerOrder get(@RestPath long id) {
        CustomerOrder order = CustomerOrder.findById(id);
        if (order == null) {
            throw new NotFoundException();
        }
        return order;
    }
}
