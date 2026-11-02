package lab.melostudy;

import java.net.URI;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
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
@Tag(name = "Orders", description = "Place orders; stock is taken atomically")
public class OrderResource {

    private final OrderService orders;

    OrderResource(OrderService orders) {
        this.orders = orders;
    }

    @POST
    @Operation(summary = "Place an order", description = "Takes the units from the stock and records the order, or does neither.")
    @APIResponse(responseCode = "201", description = "The order was placed")
    @APIResponse(responseCode = "400", ref = "ValidationError")
    @APIResponse(responseCode = "402", ref = "PaymentRefused")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "409", ref = "Conflict")
    public RestResponse<CustomerOrder> place(@Valid @NotNull OrderInput input) throws PaymentRefusedException {
        CustomerOrder order = orders.place(input.productId(), input.quantity());
        return RestResponse.ResponseBuilder.<CustomerOrder>created(URI.create("/orders/" + order.id)).entity(order).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an order by id")
    @APIResponse(responseCode = "200", description = "The order")
    @APIResponse(responseCode = "404", ref = "NotFound")
    public CustomerOrder get(@RestPath long id) {
        CustomerOrder order = CustomerOrder.findById(id);
        if (order == null) {
            throw new NotFoundException();
        }
        return order;
    }
}
