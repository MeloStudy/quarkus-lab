package lab.orders;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The orders service: an order is confirmed only if the inventory service has the stock. It first asks how much
 * there is (so it can refuse without taking anything), then reserves it.
 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    public record OrderRequest(String sku, int quantity) {
    }

    public record Order(long id, String sku, int quantity, String status) {
    }

    private final InventoryClient inventory;
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    OrderResource(InventoryClient inventory) {
        this.inventory = inventory;
    }

    @POST
    public RestResponse<Order> place(OrderRequest request) {
        if (request == null || request.sku() == null || request.sku().isBlank() || request.quantity() <= 0) {
            throw new BadRequestException("a sku and a positive quantity are required");
        }
        if (inventory.available(request.sku()) < request.quantity()) {
            throw new InventoryClient.OutOfStockException(request.sku());
        }
        inventory.reserve(request.sku(), request.quantity());
        Order order = new Order(ids.incrementAndGet(), request.sku(), request.quantity(), "CONFIRMED");
        orders.put(order.id(), order);
        return RestResponse.ResponseBuilder.<Order>created(URI.create("/orders/" + order.id())).entity(order).build();
    }

    @GET
    @Path("/{id}")
    public Order get(@RestPath long id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new NotFoundException();
        }
        return order;
    }

    @GET
    @Path("/count")
    public int count() {
        return orders.size();
    }

    @ServerExceptionMapper
    RestResponse<String> unknownSku(InventoryClient.UnknownSkuException e) {
        return RestResponse.status(422, e.getMessage());
    }

    @ServerExceptionMapper
    RestResponse<String> outOfStock(InventoryClient.OutOfStockException e) {
        return RestResponse.status(RestResponse.Status.CONFLICT, e.getMessage());
    }

    @ServerExceptionMapper
    RestResponse<String> inventoryFailed(InventoryClient.InventoryFailedException e) {
        return RestResponse.status(RestResponse.Status.BAD_GATEWAY, e.getMessage());
    }

    @ServerExceptionMapper
    RestResponse<String> inventoryTimeout(InventoryClient.InventoryTimeoutException e) {
        return RestResponse.status(RestResponse.Status.GATEWAY_TIMEOUT, e.getMessage());
    }
}
