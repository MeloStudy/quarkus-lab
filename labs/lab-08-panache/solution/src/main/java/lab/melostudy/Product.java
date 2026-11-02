package lab.melostudy;

import java.math.BigDecimal;
import java.util.Locale;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * A product of the catalog, stored in the {@code product} table.
 * Used directly as the API representation for now; lab 12 separates entity and DTO.
 * <p>Name uniqueness (case-insensitive) is a DATABASE rule: a unique constraint on {@code name_key}, the
 * lower-cased name. (Hibernate cannot declare a functional index such as {@code lower(name)}; a migration
 * could, see lab 11.)
 */
@Entity
@Table(name = "product")
@Schema(name = "Product", description = "A product of the catalog")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Server-assigned identifier", example = "6")
    public Long id;

    @Column(nullable = false, length = 100)
    @Schema(description = "Unique product name", example = "Refactoring")
    public String name;

    @Column(nullable = false)
    @Schema(description = "Product category", example = "books")
    public String category;

    @Column(nullable = false, precision = 9, scale = 2)
    @Schema(description = "Unit price", example = "42.5")
    public BigDecimal price;

    @Column(name = "name_key", nullable = false, unique = true, length = 100)
    @JsonIgnore
    @Schema(hidden = true)
    public String nameKey;

    public Product() {
    }

    public Product(String name, String category, BigDecimal price) {
        this.name = name;
        this.category = category;
        this.price = price;
    }

    @PrePersist
    @PreUpdate
    void computeNameKey() {
        nameKey = name.toLowerCase(Locale.ROOT);
    }
}
