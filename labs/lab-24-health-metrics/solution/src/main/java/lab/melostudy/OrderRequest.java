package lab.melostudy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OrderRequest(
        @NotBlank String sku,
        @Min(1) @Max(1000) int quantity,
        @Pattern(regexp = "standard|express") String type) {
}
