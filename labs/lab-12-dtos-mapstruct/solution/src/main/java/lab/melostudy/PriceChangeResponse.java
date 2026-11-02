package lab.melostudy;

import java.math.BigDecimal;
import java.time.Instant;

/** One entry of the price history of a product. */
public record PriceChangeResponse(Long id, Long productId, BigDecimal oldPrice, BigDecimal newPrice, Instant changedAt) {
}
