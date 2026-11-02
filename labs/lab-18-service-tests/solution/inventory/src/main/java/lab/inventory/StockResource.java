package lab.inventory;

import java.util.Optional;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** The inventory service: how many units of a product we have, and taking some of them. */
@Path("/stock")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StockResource {

    public record Stock(String sku, int available) {
    }

    public record ReserveRequest(int quantity) {
    }

    public record Reserved(String sku, int reserved, int remaining) {
    }

    public record Problem(String error, Integer available) {
        static Problem of(String error) {
            return new Problem(error, null);
        }
    }

    private final StockRepository stock;

    StockResource(StockRepository stock) {
        this.stock = stock;
    }

    @GET
    @Path("/{sku}")
    public RestResponse<Object> get(@RestPath String sku) {
        return stock.available(sku)
                .<RestResponse<Object>>map(n -> RestResponse.ok(new Stock(sku, n)))
                .orElseGet(() -> RestResponse.status(RestResponse.Status.NOT_FOUND, Problem.of("UNKNOWN_SKU")));
    }

    @POST
    @Path("/{sku}/reserve")
    public RestResponse<Object> reserve(@RestPath String sku, ReserveRequest request) {
        if (request == null || request.quantity() <= 0) {
            return RestResponse.status(RestResponse.Status.BAD_REQUEST, Problem.of("INVALID_QUANTITY"));
        }
        Optional<Integer> available = stock.available(sku);
        if (available.isEmpty()) {
            return RestResponse.status(RestResponse.Status.NOT_FOUND, Problem.of("UNKNOWN_SKU"));
        }
        return stock.take(sku, request.quantity())
                .<RestResponse<Object>>map(left -> RestResponse.ok(new Reserved(sku, request.quantity(), left)))
                .orElseGet(() -> RestResponse.status(RestResponse.Status.CONFLICT,
                        new Problem("INSUFFICIENT_STOCK", stock.available(sku).orElse(0))));
    }
}
