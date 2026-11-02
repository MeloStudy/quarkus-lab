package lab.melostudy;

import java.math.BigDecimal;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.RestResponse;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** REST API for exchange rates, mounted at /rates. */
@Path("/rates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RateResource {

    private final RateService rates;

    RateResource(RateService rates) {
        this.rates = rates;
    }

    @GET
    @Path("/{currency}")
    public RateResponse get(@RestPath String currency) {
        return new RateResponse(currency, rates.rateOf(currency));
    }

    @GET
    @Path("/{currency}/quote")
    public QuoteResponse quote(@RestPath String currency, @RestQuery @DefaultValue("1") int amount) {
        return new QuoteResponse(currency, amount, rates.quote(currency, amount));
    }

    @PUT
    @Path("/{currency}")
    public RateResponse change(@RestPath String currency, RateUpdate update) {
        rates.change(currency, update.rate());
        return new RateResponse(currency, update.rate());
    }

    @ServerExceptionMapper
    RestResponse<String> unknownCurrency(UnknownCurrencyException e) {
        return RestResponse.status(RestResponse.Status.NOT_FOUND, e.getMessage());
    }

    public record RateResponse(String currency, BigDecimal rate) {
    }

    public record QuoteResponse(String currency, int amount, BigDecimal total) {
    }

    public record RateUpdate(BigDecimal rate) {
    }
}
