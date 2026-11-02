package lab.melostudy;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Database access for products, in the REPOSITORY style: a separate bean that gets
 * {@code find}, {@code list}, {@code persist}, {@code delete}... from {@link PanacheRepository}.
 * Replaces the in-memory store of labs 04-07; the REST layer above must not change.
 */
@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    /** One page of products ordered by id plus the total number of matches. */
    public record ProductPage(List<Product> items, long total) {
    }

    /**
     * TODO (task 1): one page of products ordered by id, optionally filtered by category
     * (case-insensitive, null = no filter), with the total number of matches before paging.
     * Page numbers are zero-based. Let the DATABASE do the filtering and the paging.
     */
    public ProductPage page(String category, int page, int size) {
        throw new UnsupportedOperationException("TODO: ProductRepository.page");
    }

    /** TODO (task 1): the product with that id, if any. */
    public Optional<Product> findOne(long id) {
        throw new UnsupportedOperationException("TODO: ProductRepository.findOne");
    }

    /** TODO (tasks 2 and 3): store a new product (its nameKey is computed by the entity) and return it with its id. */
    public Product create(ProductInput input) {
        throw new UnsupportedOperationException("TODO: ProductRepository.create");
    }

    /**
     * TODO (tasks 2, 3 and 4): replace every field of an existing product (empty if it does not exist).
     * When the price changes, record a {@link PriceChange} in the SAME transaction.
     */
    public Optional<Product> update(long id, ProductInput input) {
        throw new UnsupportedOperationException("TODO: ProductRepository.update");
    }

    /** TODO (tasks 2 and 4): delete a product and its price history; false if it did not exist. */
    public boolean delete(long id) {
        throw new UnsupportedOperationException("TODO: ProductRepository.delete");
    }
}
