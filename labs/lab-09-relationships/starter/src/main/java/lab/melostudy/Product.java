package lab.melostudy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

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

    // TODO task 1: map this as a MANY-TO-MANY to Label, owned by Product, with a join table named
    //   product_label (columns product_id and label_id). While it is @Transient, Hibernate ignores it.
    @Transient
    @JsonIgnore
    public Set<Label> labels = new HashSet<>();

    // TODO task 2: map this as the INVERSE side of a ONE-TO-MANY to Review (the foreign key lives in
    //   review.product_id, see Review.product). Think about what should happen to the reviews when the
    //   product is removed or when a review is taken out of this list.
    @Transient
    @JsonIgnore
    public List<Review> reviews = new ArrayList<>();

    public Product() {
    }

    public Product(String name, String category, BigDecimal price) {
        this.name = name;
        this.category = category;
        this.price = price;
    }

    /** The API shows labels as plain names, sorted. Reading {@link #labels} here is what loads them. */
    @JsonProperty("labels")
    public List<String> labelNames() {
        return labels.stream().map(l -> l.name).sorted(Comparator.naturalOrder()).toList();
    }

    @PrePersist
    @PreUpdate
    void computeNameKey() {
        nameKey = name.toLowerCase(Locale.ROOT);
    }
}
