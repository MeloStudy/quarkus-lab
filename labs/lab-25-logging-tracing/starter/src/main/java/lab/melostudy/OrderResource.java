package lab.melostudy;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderService service;

    @Inject
    OrderStore store;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid OrderRequest request) {
        Order order = service.create(request);
        return Response.created(URI.create("/api/orders/" + order.id())).entity(order).build();
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") long id) {
        return store.find(id).map(order -> Response.ok(order).build()).orElse(Response.status(404).build());
    }
}
