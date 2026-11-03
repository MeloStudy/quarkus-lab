package lab.melostudy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderRequest(
        @NotBlank String sku,
        @Min(1) @Max(1000) int quantity,
        @NotBlank @Email String customerEmail) {
}
