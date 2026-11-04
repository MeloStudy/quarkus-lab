package lab.pricing;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

/** Endpoints that the library brings with it: any application that includes the library serves them. */
@Path("/prices")
public class PricingResource {

    @Inject
    PriceCalculator calculator;

    @Inject
    CallCounter counter;

    @GET
    @Path("/total")
    @Produces("text/plain")
    public String total(@QueryParam("quantity") int quantity, @QueryParam("unit") long unit) {
        return String.valueOf(calculator.total(quantity, unit));
    }

    /** How many calls to {@link Logged} methods the application has made (by any bean, in any module). */
    @GET
    @Path("/calls")
    @Produces("text/plain")
    public String calls() {
        return String.valueOf(counter.calls());
    }
}
