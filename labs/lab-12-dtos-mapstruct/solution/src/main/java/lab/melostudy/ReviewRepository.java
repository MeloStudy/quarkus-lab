package lab.melostudy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Reviews of a product. One query on {@link Review} returns BOTH kinds (polymorphic query). */
@ApplicationScoped
public class ReviewRepository implements PanacheRepository<Review> {

    private final ProductRepository products;

    ReviewRepository(ProductRepository products) {
        this.products = products;
    }

    /** Empty when the product does not exist. */
    @Transactional
    public Optional<Review> add(long productId, ReviewInput input) {
        Product product = products.findById(productId);
        if (product == null) {
            return Optional.empty();
        }
        Review review = switch (input) {
            case ReviewInput.Customer c -> {
                CustomerReview r = new CustomerReview();
                r.verifiedPurchase = c.verifiedPurchase();
                yield r;
            }
            case ReviewInput.Critic c -> {
                CriticReview r = new CriticReview();
                r.outlet = c.outlet();
                yield r;
            }
        };
        review.rating = input.rating();
        review.comment = input.comment();
        review.createdAt = Instant.now();
        review.product = product;
        product.reviews.add(review); // keep BOTH sides of the relationship in sync
        persist(review);
        return Optional.of(review);
    }

    /** Newest first. */
    public List<Review> of(long productId) {
        return list("product.id", Sort.descending("id"), productId);
    }

    public ReviewSummary summaryOf(long productId) {
        Object[] row = getEntityManager()
                .createQuery("select count(r), avg(r.rating) from Review r where r.product.id = :id", Object[].class)
                .setParameter("id", productId)
                .getSingleResult();
        long count = (Long) row[0];
        double average = row[1] == null ? 0.0 : Math.round(((Double) row[1]) * 100) / 100.0;
        return new ReviewSummary(count, average);
    }
}
