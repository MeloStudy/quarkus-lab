package lab.melostudy;

import java.math.BigDecimal;

/** What the API accepts when creating or updating a product (no id: the server assigns it). */
public record ProductInput(String name, String category, BigDecimal price) {
}
