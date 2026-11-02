package lab.melostudy;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** What the API accepts when placing an order. */
@Schema(description = "Data needed to place an order")
public record OrderInput(
        @Schema(description = "The product to buy", example = "1") @NotNull Long productId,
        @Schema(description = "Units to buy (1 to 50)", example = "2") @Min(1) @Max(50) int quantity) {
}
