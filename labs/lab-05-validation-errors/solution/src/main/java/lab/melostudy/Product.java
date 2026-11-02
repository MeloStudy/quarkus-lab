package lab.melostudy;

import java.math.BigDecimal;

/** What the API returns. */
public record Product(Long id, String name, String category, BigDecimal price) {
}
