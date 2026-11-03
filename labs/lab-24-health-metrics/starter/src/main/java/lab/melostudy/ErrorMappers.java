package lab.melostudy;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.Response;

public class ErrorMappers {

    @ServerExceptionMapper
    public Response unavailable(InventoryUnavailableException e) {
        return Response.status(503).entity(new ApiError("INVENTORY_UNAVAILABLE", e.getMessage())).build();
    }

    @ServerExceptionMapper
    public Response outOfStock(OutOfStockException e) {
        return Response.status(409).entity(new ApiError("OUT_OF_STOCK", e.getMessage())).build();
    }
}
