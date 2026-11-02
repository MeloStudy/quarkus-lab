package lab.melostudy;

import java.math.BigDecimal;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** A product price converted to another currency. */
@Schema(description = "A product price converted to another currency")
public record PriceQuote(
        @Schema(description = "Product identifier", example = "1") long productId,
        @Schema(description = "Requested currency (ISO 4217)", example = "EUR") String currency,
        @Schema(description = "Exchange rate used (units of the currency per USD)", example = "0.92") BigDecimal rate,
        @Schema(description = "Price converted to the currency, 2 decimals", example = "28.06") BigDecimal amount) {
}
