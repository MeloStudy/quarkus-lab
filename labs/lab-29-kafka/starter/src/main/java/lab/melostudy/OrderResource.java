package lab.melostudy;

import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lab.melostudy.Exceptions.InvalidOrderException;

/**
 * The door of the service. An order is not shipped here: it is published to Kafka, and the answer is {@code 202 Accepted}
 * <b>once the broker has acknowledged the record</b>. Shipping happens later, in another consumer group.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    // TODO task 1: inject the emitter of the "orders-out" channel. The Kafka record needs a key (the customer).

    @Inject
    ShipmentStore store;

    @ConfigProperty(name = "app.orders.ack-timeout-millis", defaultValue = "10000")
    long ackTimeoutMillis;

    public record Placed(String id) {
    }

    @POST
    @Path("/orders")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> place(OrderRequest request) {
        if (request.customer() == null || request.customer().isBlank()) {
            throw new InvalidOrderException("The customer is required");
        }
        if (request.quantity() <= 0) {
            throw new InvalidOrderException("The quantity must be positive");
        }
        if (!Catalog.SKUS.contains(request.sku())) {
            throw new InvalidOrderException("Unknown sku " + request.sku());
        }
        Order order = new Order(UUID.randomUUID().toString(), request.customer(), request.sku(), request.quantity(), request.note());
        // TODO task 1: publish the order with the customer as the key and answer 202 (body Placed) once the broker has
        //   acknowledged the record. If the broker refuses it, the failure reaches ErrorMappers (502). If nobody answers
        //   within ackTimeoutMillis, throw NoAckException (504).
        return Uni.createFrom().item(Response.status(501).build());
    }

    @GET
    @Path("/shipments/{id}")
    public Order shipment(@PathParam("id") String id) {
        return store.find(id).orElseThrow(NotFoundException::new);
    }
}
