package lab.melostudy;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.Response;

public class ErrorMappers {

    // TODO Task 5: say what happened in the log, at the right level.
    /** A normal business outcome: the caller asked for more than there is. */
    @ServerExceptionMapper
    public Response outOfStock(OutOfStockException e) {
        return Response.status(409).entity(new ApiError("OUT_OF_STOCK", e.getMessage())).build();
    }

    /** Our dependency failed. */
    @ServerExceptionMapper
    public Response stockUnavailable(StockUnavailableException e) {
        return Response.status(502).entity(new ApiError("STOCK_UNAVAILABLE", e.getMessage())).build();
    }
}
