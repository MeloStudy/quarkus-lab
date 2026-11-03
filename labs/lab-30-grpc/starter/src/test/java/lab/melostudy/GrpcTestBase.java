package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcClientUtils;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import lab.melostudy.grpc.GetProductRequest;
import lab.melostudy.grpc.ImportSummary;
import lab.melostudy.grpc.ListProductsRequest;
import lab.melostudy.grpc.MutinyCatalogGrpc;
import lab.melostudy.grpc.Product;

/** A real gRPC client against the real server of the application (port 9001 in tests), with the API key in the metadata. */
abstract class GrpcTestBase {

    static final Duration WAIT = Duration.ofSeconds(10);

    @GrpcClient("catalog")
    MutinyCatalogGrpc.MutinyCatalogStub withoutKey;

    @Inject
    ProductRepository repository;

    @Inject
    ServerStats stats;

    @BeforeEach
    void cleanServer() {
        repository.reset();
        stats.reset();
    }

    /** The client stub that sends the right key. */
    MutinyCatalogGrpc.MutinyCatalogStub catalog() {
        return withKey("test-key");
    }

    MutinyCatalogGrpc.MutinyCatalogStub withKey(String key) {
        Metadata headers = new Metadata();
        headers.put(ApiKeyInterceptor.API_KEY, key);
        return GrpcClientUtils.attachHeaders(withoutKey, headers);
    }

    Product get(String id) {
        return catalog().getProduct(GetProductRequest.newBuilder().setId(id).build()).await().atMost(WAIT);
    }

    List<Product> list(int count) {
        return catalog().listProducts(ListProductsRequest.newBuilder().setCount(count).build()).collect().asList().await().atMost(WAIT);
    }

    ImportSummary importAll(Product... products) {
        return catalog().importProducts(Multi.createFrom().items(products)).await().atMost(WAIT);
    }

    static Product product(String id, String name) {
        return Product.newBuilder().setId(id).setName(name).setPriceCents(500).build();
    }

    /** The status code the server answered with. */
    static Status.Code codeOf(Supplier<?> call) {
        return assertThrows(StatusRuntimeException.class, call::get).getStatus().getCode();
    }

    static String descriptionOf(Supplier<?> call) {
        String description = assertThrows(StatusRuntimeException.class, call::get).getStatus().getDescription();
        return description == null ? "" : description;
    }
}
