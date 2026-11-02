package lab.melostudy;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** What the API accepts when creating or updating a product (no id: the server assigns it). */
public record ProductInput(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @AllowedCategory String category,
        @NotNull @Positive @Digits(integer = 7, fraction = 2) BigDecimal price) {
}
