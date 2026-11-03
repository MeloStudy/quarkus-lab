package lab.melostudy;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.Response;

public class ErrorMappers {

    private static final Logger LOG = Logger.getLogger(ErrorMappers.class);

    /** A normal business outcome: the caller asked for more than there is. Not a warning, not an error. */
    @ServerExceptionMapper
    public Response outOfStock(OutOfStockException e) {
        LOG.info("Order refused: not enough stock");
        return Response.status(409).entity(new ApiError("OUT_OF_STOCK", e.getMessage())).build();
    }

    /** Our dependency failed. Somebody on call will want to see it, and the mapper is where it stops being an exception. */
    @ServerExceptionMapper
    public Response stockUnavailable(StockUnavailableException e) {
        LOG.warnf("Order failed: %s", e.getMessage());
        return Response.status(502).entity(new ApiError("STOCK_UNAVAILABLE", e.getMessage())).build();
    }
}
