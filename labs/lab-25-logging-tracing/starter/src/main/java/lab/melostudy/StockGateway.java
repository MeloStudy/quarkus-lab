package lab.melostudy;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** The business step "reserve the stock". */
@ApplicationScoped
public class StockGateway {

    @Inject
    @RestClient
    StockClient stock;

    // TODO Task 4: make this step a span of its own, named "reserve-stock", with the SKU as the attribute "order.sku".
    public void reserve(String sku, int quantity) {
        stock.reserve(new ReserveRequest(sku, quantity));
    }
}
