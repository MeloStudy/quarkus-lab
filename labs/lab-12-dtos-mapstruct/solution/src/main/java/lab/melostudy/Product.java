package lab.melostudy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * A product of the catalog, stored in the {@code product} table.
 * A persistence class only: the API shows {@link ProductResponse}, never this entity.
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

    @Column(nullable = false)
    public int stock;

    /** Internal: what the shop pays for the product. Must never reach the API. */
    @Column(name = "cost_price", precision = 9, scale = 2)
    public BigDecimal costPrice;

    @Column(name = "name_key", nullable = false, unique = true, length = 100)
    public String nameKey;

    /** MANY-TO-MANY, owning side: the join table {@code product_label} is declared here. */
    @ManyToMany
    @JoinTable(name = "product_label",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id"))
    public Set<Label> labels = new HashSet<>();

    /** ONE-TO-MANY, inverse side: the foreign key is in {@code review.product_id}, hence {@code mappedBy}. */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Review> reviews = new ArrayList<>();

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
        category = category.trim().toLowerCase(Locale.ROOT);
    }
}
