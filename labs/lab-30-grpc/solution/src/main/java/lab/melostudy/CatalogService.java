package lab.melostudy;

import java.time.Duration;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import lab.melostudy.grpc.GetProductRequest;
import lab.melostudy.grpc.ImportSummary;
import lab.melostudy.grpc.ListProductsRequest;
import lab.melostudy.grpc.MutinyCatalogGrpc;
import lab.melostudy.grpc.Product;

/**
 * The server side of {@code catalog.proto}. The base class is generated; each method is one rpc, in the Mutiny flavour
 * ({@code Uni} for one message, {@code Multi} for a stream). A failure is reported to the caller with a gRPC
 * {@link Status}: the code is what the client programs against.
 */
@GrpcService
public class CatalogService extends MutinyCatalogGrpc.CatalogImplBase {

    private static final int MAX_COUNT = 1000;

    @Inject
    ProductRepository repository;

    @Inject
    ServerStats stats;

    /** The data source blocks, so this method must not run on the event loop: {@code @Blocking} moves it to a worker. */
    @Override
    @Blocking
    public Uni<Product> getProduct(GetProductRequest request) {
        stats.getProductRanOn(Thread.currentThread().getName());
        if (request.getId().isBlank()) {
            return Uni.createFrom().failure(Status.INVALID_ARGUMENT.withDescription("The id is required").asRuntimeException());
        }
        return repository.find(request.getId()).map(product -> Uni.createFrom().item(product)).orElseGet(
                () -> Uni.createFrom().failure(Status.NOT_FOUND.withDescription("No product " + request.getId()).asRuntimeException()));
    }

    /** One product every 10 ms (a slow producer). When the client stops listening, the stream must stop too. */
    @Override
    public Multi<Product> listProducts(ListProductsRequest request) {
        int count = request.getCount();
        if (count < 1 || count > MAX_COUNT) {
            return Multi.createFrom().failure(Status.INVALID_ARGUMENT.withDescription("count must be between 1 and " + MAX_COUNT).asRuntimeException());
        }
        return Multi.createFrom().ticks().every(Duration.ofMillis(10)).select().first(count)
                .onItem().invoke(stats::productStreamed)
                .onCancellation().invoke(stats::streamCancelled)
                .map(i -> Product.newBuilder().setId("n" + i).setName("Streamed " + i).setPriceCents(100 + i).build());
    }

    /** All or nothing: nothing is stored unless every product of the stream is valid. */
    @Override
    public Uni<ImportSummary> importProducts(Multi<Product> request) {
        return request.onItem().invoke(product -> {
            if (product.getId().isBlank() || product.getName().isBlank()) {
                throw Status.INVALID_ARGUMENT.withDescription("Product '" + product.getId() + "' needs an id and a name").asRuntimeException();
            }
        }).collect().asList().onItem().transform(products -> {
            repository.addAll(products);
            return ImportSummary.newBuilder().setImported(products.size()).build();
        });
    }
}
