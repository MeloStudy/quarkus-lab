package lab.melostudy;

import java.math.BigDecimal;
import java.time.Instant;

/** An accepted order as the API shows it. */
public record OrderResponse(Long id, Long productId, int quantity, BigDecimal total, Instant createdAt) {
}
