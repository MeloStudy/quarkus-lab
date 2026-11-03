package lab.melostudy;

import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class QuoteResource {

    @Inject
    QuoteService service;

    @Inject
    CatalogService catalog;

    @Inject
    SensorService sensors;

    @Inject
    AuditTrail audit;

    /** Task 1: every request is recorded before it is answered. */
    @GET
    @Path("quotes/{sku}")
    public Uni<Quote> best(@PathParam("sku") String sku) {
        return audit.record("quote requested for " + sku)
                .chain(() -> service.best(sku));
    }

    @GET
    @Path("quotes")
    public Multi<Quote> bestOfAll(@QueryParam("skus") List<String> skus) {
        return catalog.bestOfAll(skus);
    }

    @GET
    @Path("readings")
    public Multi<Long> readings(@QueryParam("count") @DefaultValue("5") int count) {
        return sensors.readings().select().first(count);
    }
}
