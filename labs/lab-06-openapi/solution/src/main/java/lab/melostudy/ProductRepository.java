package lab.melostudy;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-memory store (real persistence arrives in lab 08). Already implemented: the
 * lab is about validation and errors. Product names are unique: creating or updating
 * with a taken name throws {@link DuplicateProductException}. Ids 1-5 are seeded; new products get 6, 7, ...
 */
@ApplicationScoped
public class ProductRepository {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @PostConstruct
    public void reset() {
        products.clear();
        sequence.set(0);
        create(new ProductInput("Clean Code", "books", new BigDecimal("30.5")));
        create(new ProductInput("Effective Java", "books", new BigDecimal("45.0")));
        create(new ProductInput("Domain-Driven Design", "books", new BigDecimal("50.25")));
        create(new ProductInput("Chess", "games", new BigDecimal("20.0")));
        create(new ProductInput("Catan", "games", new BigDecimal("40.5")));
    }

    /** All products ordered by id, optionally filtered by category (null = no filter). */
    public List<Product> findAll(String category) {
        return products.values().stream()
                .filter(p -> category == null || p.category().equalsIgnoreCase(category))
                .sorted(Comparator.comparing(Product::id))
                .toList();
    }

    public Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    public synchronized Product create(ProductInput input) {
        ensureUniqueName(input.name(), null);
        Product product = new Product(sequence.incrementAndGet(), input.name(), input.category(), input.price());
        products.put(product.id(), product);
        return product;
    }

    public synchronized Optional<Product> update(long id, ProductInput input) {
        if (!products.containsKey(id)) {
            return Optional.empty();
        }
        ensureUniqueName(input.name(), id);
        Product updated = new Product(id, input.name(), input.category(), input.price());
        return Optional.ofNullable(products.computeIfPresent(id, (key, old) -> updated));
    }

    private void ensureUniqueName(String name, Long ownId) {
        boolean taken = products.values().stream()
                .anyMatch(p -> p.name().equalsIgnoreCase(name) && !p.id().equals(ownId));
        if (taken) {
            throw new DuplicateProductException(name);
        }
    }

    public boolean delete(long id) {
        return products.remove(id) != null;
    }
}
