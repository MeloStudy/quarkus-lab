package lab.melostudy;

import java.math.BigDecimal;
import java.util.Locale;

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
 * <p>Name uniqueness (case-insensitive) must be a DATABASE rule (task 3).
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(nullable = false)
    public String category;

    @Column(nullable = false, precision = 9, scale = 2)
    public BigDecimal price;

    // TODO (task 3): product names are unique (case-insensitive). This column holds the lower-cased name;
    // make the DATABASE enforce its uniqueness.
    @Column(name = "name_key", nullable = false, length = 100)
    @JsonIgnore
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
