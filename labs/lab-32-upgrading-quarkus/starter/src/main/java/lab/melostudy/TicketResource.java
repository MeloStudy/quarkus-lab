package lab.melostudy;

import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/** A small support-desk service, written for an older Quarkus and untouched since. */
@Path("/tickets")
@Produces(MediaType.APPLICATION_JSON)
public class TicketResource {

    public record Ticket(String id, String title) {
    }

    private static final Map<String, Ticket> TICKETS = Map.of("t1", new Ticket("t1", "Printer on fire"));

    @Inject
    MeterRegistry registry;

    @GET
    @Path("/{id}")
    public Ticket find(@PathParam("id") String id) {
        Ticket ticket = TICKETS.get(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        registry.counter("tickets.opened", "channel", "web").increment();
        return ticket;
    }

    /** Slow because it talks to a legacy system with a blocking client; it is transactional and returns a Uni. */
    @GET
    @Path("/slow")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Uni<String> slow() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Uni.createFrom().item(Thread.currentThread().getName());
    }

    /** The same counter, now also counted per region. */
    @GET
    @Path("/regional")
    @Produces(MediaType.TEXT_PLAIN)
    public String regional(@QueryParam("region") String region) {
        registry.counter("tickets.opened", "region", region).increment();
        return "counted " + region;
    }
}
