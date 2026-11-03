package lab.melostudy;

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

/**
 * The door of the order desk. The order is not handled here: it is put on the {@code orders} channel, and the answer is
 * decided by what the channel says back: an ack (201), a nack (the failure of whichever stage rejected it, mapped in
 * {@link ErrorMappers}), or nothing in time (504).
 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    // TODO task 1: inject an Emitter<Order> for the "orders" channel.
    // TODO task 4: give it an explicit @OnOverflow buffer of 10 orders.

    @Inject
    Ledger ledger;

    /** How long to wait for an ack or a nack before answering 504 (see application.properties). */
    @ConfigProperty(name = "app.orders.ack-timeout-millis", defaultValue = "5000")
    long ackTimeoutMillis;

    /** The body of a 201. */
    public record Placed(String id, OrderStatus status) {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> place(OrderRequest request) {
        // TODO task 1: give the order an id (UUID), send it to the channel and answer 201 (Location: /orders/{id}, body Placed)
        //   when the channel acknowledges it. A nack must come out as the exception the stage threw (ErrorMappers maps it),
        //   and nobody answering within ackTimeoutMillis is a NoAckException (504).
        // TODO task 4: when the channel has no room, send() throws: answer 503 by throwing BusyException instead. A channel
        //   that was cancelled throws the same exception, but that one is not "busy": do not hide it.
        return Uni.createFrom().item(Response.status(501).build());
    }

    @GET
    @Path("/{id}")
    public Ledger.Entry find(@PathParam("id") String id) {
        return ledger.find(id).orElseThrow(NotFoundException::new);
    }
}
