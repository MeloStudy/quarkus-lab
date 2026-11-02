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
 * <p>Name uniqueness (case-insensitive) is a DATABASE rule: a unique constraint on {@code name_key}, the
 * lower-cased name. (Hibernate cannot declare a functional index such as {@code lower(name)}; a migration
 * could, see lab 11.)
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

    @Column(name = "name_key", nullable = false, unique = true, length = 100)
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
