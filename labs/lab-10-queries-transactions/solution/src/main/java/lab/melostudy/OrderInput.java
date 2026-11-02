package lab.melostudy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** What the API accepts when placing an order. */
public record OrderInput(
        @NotNull Long productId,
        @Min(1) @Max(50) int quantity) {
}
