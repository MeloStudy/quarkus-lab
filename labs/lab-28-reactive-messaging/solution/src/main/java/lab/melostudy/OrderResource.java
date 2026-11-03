package lab.melostudy;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Produces;
import lab.melostudy.Exceptions.BusyException;
import lab.melostudy.Exceptions.NoAckException;

/**
 * The door of the order desk. The order is not handled here: it is put on the {@code orders} channel, and the answer is
 * decided by what the channel says back: an ack (201), a nack (the failure of whichever stage rejected it, mapped in
 * {@link ErrorMappers}), or nothing in time (504).
 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    @Channel("orders")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10)
    Emitter<Order> orders;

    @Inject
    Ledger ledger;

    @ConfigProperty(name = "app.orders.ack-timeout-millis", defaultValue = "5000")
    long ackTimeoutMillis;

    public record Placed(String id, OrderStatus status) {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> place(OrderRequest request) {
        Order order = new Order(UUID.randomUUID().toString(), request.sku(), request.quantity());
        Uni<Void> acknowledged;
        try {
            acknowledged = Uni.createFrom().completionStage(orders.send(order));
        } catch (IllegalStateException e) {
            // send() throws when the buffer is full. A channel that was cancelled throws the same exception, but that is a
            // broken application, not a busy one: it must not be hidden behind a 503.
            if (orders.isCancelled()) {
                throw e;
            }
            throw new BusyException("The order desk is full, try again in a moment");
        }
        return acknowledged.ifNoItem().after(Duration.ofMillis(ackTimeoutMillis)).failWith(() -> new NoAckException("Nobody answered for order " + order.id()))
                .map(done -> {
                    OrderStatus status = ledger.find(order.id()).map(Ledger.Entry::status).orElse(null);
                    return Response.created(URI.create("/orders/" + order.id())).entity(new Placed(order.id(), status)).build();
                });
    }

    @GET
    @Path("/{id}")
    public Ledger.Entry find(@PathParam("id") String id) {
        return ledger.find(id).orElseThrow(NotFoundException::new);
    }
}
