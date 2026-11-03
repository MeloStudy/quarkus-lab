package lab.melostudy;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
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

    @Inject
    ProductRepository repository;

    @Inject
    ServerStats stats;

    // TODO task 1: answer with the product; a blank id is INVALID_ARGUMENT and an unknown one is NOT_FOUND (Status.<CODE>.withDescription(...).asRuntimeException()).
    // TODO task 3: the repository blocks (30 ms per lookup). Call stats.getProductRanOn(Thread.currentThread().getName()) first, run the test and read the thread name.
    @Override
    public Uni<Product> getProduct(GetProductRequest request) {
        return Uni.createFrom().failure(Status.UNIMPLEMENTED.asRuntimeException());
    }

    // TODO task 2: stream request.getCount() products, one every 10 ms (ids "n0", "n1"...; stats.productStreamed() for each one sent,
    //   stats.streamCancelled() when the client stops listening). A count outside 1..1000 is INVALID_ARGUMENT.
    @Override
    public Multi<Product> listProducts(ListProductsRequest request) {
        return Multi.createFrom().failure(Status.UNIMPLEMENTED.asRuntimeException());
    }

    // TODO task 2: all or nothing. A product without id or name is INVALID_ARGUMENT and nothing from that call is stored
    //   (repository.addAll(...) only when the whole stream was valid). The answer says how many were imported.
    @Override
    public Uni<ImportSummary> importProducts(Multi<Product> request) {
        return Uni.createFrom().failure(Status.UNIMPLEMENTED.asRuntimeException());
    }
}
