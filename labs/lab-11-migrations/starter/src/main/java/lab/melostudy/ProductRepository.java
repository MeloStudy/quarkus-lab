package lab.melostudy;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.exception.ConstraintViolationException;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
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

    public ProductPage page(String category, String label, String text, BigDecimal minPrice, BigDecimal maxPrice,
            int page, int size) {
        StringBuilder where = new StringBuilder();
        Parameters params = new Parameters();
        if (category != null) {
            where.append("lower(p.category) = :category");
            params.and("category", category.toLowerCase(Locale.ROOT));
        }
        if (label != null) {
            and(where);
            where.append("exists (select 1 from p.labels l where l.name = :label)");
            params.and("label", label);
        }
        if (text != null) {
            and(where);
            // locate() is a plain substring search: unlike LIKE, a "%" or "_" typed by the user means itself
            where.append("locate(lower(:text), lower(p.name)) > 0");
            params.and("text", text);
        }
        if (minPrice != null) {
            and(where);
            where.append("p.price >= :minPrice");
            params.and("minPrice", minPrice);
        }
        if (maxPrice != null) {
            and(where);
            where.append("p.price <= :maxPrice");
            params.and("maxPrice", maxPrice);
        }
        PanacheQuery<Product> query = where.isEmpty()
                ? findAll(Sort.by("id"))
                : find("from Product p where " + where, Sort.by("id"), params);
        long total = query.count();
        List<Product> items = query.page(Page.of(page, size)).list();
        return new ProductPage(items, total);
    }

    private static void and(StringBuilder where) {
        if (!where.isEmpty()) {
            where.append(" and ");
        }
    }

    /** Aggregates computed by the database: one row per category, no product is loaded. */
    public List<CategoryStats> categoryStats() {
        return getEntityManager().createQuery(
                "select new lab.melostudy.CategoryStats(p.category, count(p), sum(p.stock), round(avg(p.price), 2)) "
                        + "from Product p group by p.category order by p.category", CategoryStats.class)
                .getResultList();
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
        // The response shows the labels, and the REST layer serializes it AFTER this transaction (and its
        // session) is closed: load the lazy collection now or serialization fails with LazyInitializationException.
        Hibernate.initialize(product.labels);
        if (oldPrice.compareTo(input.price()) != 0) {
            PriceChange.of(id, oldPrice, input.price()).persist();
        }
        return Optional.of(product);
    }

    /** Replaces the labels of a product with exactly these names; unknown names are created. */
    @Transactional
    public Optional<Product> setLabels(long id, Collection<String> names) {
        Product product = findById(id);
        if (product == null) {
            return Optional.empty();
        }
        Set<Label> wanted = new HashSet<>();
        for (String name : names) {
            wanted.add(Label.findByName(name).orElseGet(() -> {
                Label created = Label.named(name);
                created.persist();
                return created;
            }));
        }
        product.labels.clear();
        product.labels.addAll(wanted);
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
