package lab.melostudy;

import java.math.BigDecimal;
import java.util.List;

/** What the API shows of a product. Adding a field to the entity changes nothing here until someone adds it on purpose. */
public record ProductResponse(Long id, String name, String category, BigDecimal price, int stock, List<String> labels) {
}
