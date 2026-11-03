package lab.melostudy;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** The business step "reserve the stock", worth a span of its own in a trace. */
@ApplicationScoped
public class StockGateway {

    @Inject
    @RestClient
    StockClient stock;

    @WithSpan("reserve-stock")
    public void reserve(@SpanAttribute("order.sku") String sku, int quantity) {
        stock.reserve(new ReserveRequest(sku, quantity));
    }
}
