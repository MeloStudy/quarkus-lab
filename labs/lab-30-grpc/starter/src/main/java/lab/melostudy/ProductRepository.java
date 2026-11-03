package lab.melostudy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import lab.melostudy.grpc.Product;

/**
 * A slow, blocking data source (think JDBC): every lookup takes 30 ms and blocks the calling thread. The id {@code boom}
 * simulates a bug: the data source throws something nobody expected.
 */
@ApplicationScoped
public class ProductRepository {

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public ProductRepository() {
        reset();
    }

    public Optional<Product> find(String id) {
        pause();
        if ("boom".equals(id)) {
            throw new IllegalStateException("connection to db.internal.example:5432 failed, password hunter2");
        }
        return Optional.ofNullable(products.get(id));
    }

    public void addAll(List<Product> imported) {
        imported.forEach(product -> products.put(product.getId(), product));
    }

    /** Back to the five products every test starts with. */
    public final void reset() {
        products.clear();
        for (int i = 1; i <= 5; i++) {
            products.put("p" + i, Product.newBuilder().setId("p" + i).setName("Product " + i).setPriceCents(1000L * i).build());
        }
    }

    private static void pause() {
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
