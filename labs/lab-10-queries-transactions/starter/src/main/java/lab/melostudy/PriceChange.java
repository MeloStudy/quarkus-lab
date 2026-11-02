package lab.melostudy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One row per price change of a product. This entity uses the ACTIVE RECORD style:
 * the entity itself carries the queries ({@link #historyOf(long)}), and {@link PanacheEntity}
 * provides the {@code id} and the static helpers ({@code list}, {@code delete}, ...).
 * Compare with {@link ProductRepository}, which uses the REPOSITORY style.
 */
@Entity
@Table(name = "price_change")
@Schema(description = "A recorded change of a product price")
public class PriceChange extends PanacheEntity {

    @Column(nullable = false)
    @Schema(description = "The product whose price changed", example = "1")
    public Long productId;

    @Column(nullable = false, precision = 9, scale = 2)
    public BigDecimal oldPrice;

    @Column(nullable = false, precision = 9, scale = 2)
    public BigDecimal newPrice;

    @Column(nullable = false)
    public Instant changedAt;

    public static PriceChange of(long productId, BigDecimal oldPrice, BigDecimal newPrice) {
        PriceChange change = new PriceChange();
        change.productId = productId;
        change.oldPrice = oldPrice;
        change.newPrice = newPrice;
        change.changedAt = Instant.now();
        return change;
    }

    /** Changes of one product, newest first. */
    public static List<PriceChange> historyOf(long productId) {
        return list("productId", Sort.descending("id"), productId);
    }
}
