package lab.melostudy;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/** Tasks 2 to 4: prices, served from a slow blocking driver. */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class PriceResource {

    public record Total(String sku, int quantity, long cents) {
    }

    public record Priced(String sku, long cents, String kind) {
    }

    @Inject
    LegacyCatalog catalog;

    @Inject
    PriceCache cache;

    /** Answers at once and never blocks. If it is slow, the event loop is busy with something else. */
    @GET
    @Path("ping")
    public Uni<String> ping() {
        return Uni.createFrom().item("pong");
    }

    /** Task 2: one price, from the slow driver. */
    @GET
    @Path("prices/{sku}")
    @Blocking
    public Uni<Price> price(@PathParam("sku") String sku) {
        return Uni.createFrom().item(() -> catalog.lookup(sku));
    }

    /** Task 2: the price times a quantity. It needs the value, so it waits for it. */
    @GET
    @Path("prices/{sku}/total")
    @Blocking
    public Uni<Total> total(@PathParam("sku") String sku, @QueryParam("quantity") int quantity) {
        Price price = price(sku).await().indefinitely();
        return Uni.createFrom().item(new Total(sku, quantity, price.cents() * quantity));
    }

    /** Task 3: a blocking call that many requests make at the same time. */
    @GET
    @Path("prices/{sku}/blocking")
    @RunOnVirtualThread
    public Priced blocking(@PathParam("sku") String sku) {
        Price price = catalog.lookup(sku);
        return new Priced(price.sku(), price.cents(), Where.here().kind());
    }

    /** Task 4: the same, through a cache that holds a lock while it asks the driver. */
    @GET
    @Path("prices/{sku}/cached")
    @RunOnVirtualThread
    public Priced cached(@PathParam("sku") String sku) {
        Price price = cache.get(sku);
        return new Priced(price.sku(), price.cents(), Where.here().kind());
    }

    @GET
    @Path("driver/calls")
    @Produces(MediaType.TEXT_PLAIN)
    public String driverCalls() {
        return Integer.toString(catalog.calls());
    }
}
