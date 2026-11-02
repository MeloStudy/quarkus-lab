package lab.melostudy;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

/**
 * Database access for products, in the REPOSITORY style: a separate bean that gets
 * {@code find}, {@code list}, {@code persist}, {@code delete}... from {@link PanacheRepository}.
 * Product names are unique (case-insensitive) because the DATABASE says so; this class only
 * translates the violation into {@link DuplicateProductException}.
 */
@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    /** One page of products ordered by id plus the total number of matches. */
    public record ProductPage(List<Product> items, long total) {
    }

    public ProductPage page(String category, int page, int size) {
        PanacheQuery<Product> query = category == null
                ? findAll(Sort.by("id"))
                : find("lower(category) = ?1", Sort.by("id"), category.toLowerCase(Locale.ROOT));
        long total = query.count();
        List<Product> items = query.page(Page.of(page, size)).list();
        return new ProductPage(items, total);
    }

    public Optional<Product> findOne(long id) {
        return findByIdOptional(id);
    }

    @Transactional
    public Product create(ProductInput input) {
        Product product = new Product(input.name(), input.category(), input.price());
        translatingDuplicates(() -> persistAndFlush(product), input.name());
        return product;
    }

    /** Replaces every field; records a {@link PriceChange} in the SAME transaction when the price changes. */
    @Transactional
    public Optional<Product> update(long id, ProductInput input) {
        Product product = findById(id);
        if (product == null) {
            return Optional.empty();
        }
        var oldPrice = product.price;
        product.name = input.name();
        product.category = input.category();
        product.price = input.price();
        translatingDuplicates(this::flush, input.name());
        if (oldPrice.compareTo(input.price()) != 0) {
            PriceChange.of(id, oldPrice, input.price()).persist();
        }
        return Optional.of(product);
    }

    @Transactional
    public boolean delete(long id) {
        PriceChange.delete("productId", id);
        return deleteById(id);
    }

    /**
     * Runs a database write NOW (flush) and turns a unique-constraint violation into a domain exception.
     * Without the flush the violation would only surface when the transaction commits, after this
     * method has returned, and nobody here could translate it.
     */
    private void translatingDuplicates(Runnable write, String name) {
        try {
            write.run();
        } catch (PersistenceException e) {
            if (isUniqueViolation(e)) {
                throw new DuplicateProductException(name);
            }
            throw e;
        }
    }

    private static boolean isUniqueViolation(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve
                    && cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {
                return true;
            }
        }
        return false;
    }
}
