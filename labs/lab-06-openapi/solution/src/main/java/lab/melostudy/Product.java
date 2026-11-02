package lab.melostudy;

import java.math.BigDecimal;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** What the API returns. */
@Schema(description = "A product of the catalog")
public record Product(
        @Schema(description = "Server-assigned identifier", example = "6") Long id,
        @Schema(description = "Unique product name", example = "Refactoring") String name,
        @Schema(description = "Product category", example = "books") String category,
        @Schema(description = "Unit price", example = "42.5") BigDecimal price) {
}
