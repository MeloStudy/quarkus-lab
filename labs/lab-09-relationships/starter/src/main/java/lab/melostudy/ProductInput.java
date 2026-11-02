package lab.melostudy;

import java.math.BigDecimal;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** What the API accepts when creating or updating a product (no id: the server assigns it). */
@Schema(description = "Data needed to create or replace a product")
public record ProductInput(
        @Schema(description = "Unique product name (case-insensitive)", example = "Refactoring")
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "One of the configured categories", example = "books")
        @NotBlank @AllowedCategory String category,
        @Schema(description = "Unit price, greater than zero, at most 2 decimals", example = "42.5")
        @NotNull @Positive @Digits(integer = 7, fraction = 2) BigDecimal price) {
}
