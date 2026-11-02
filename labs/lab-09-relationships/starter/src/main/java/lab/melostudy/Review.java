package lab.melostudy;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * A review of a product. There are two kinds ({@link CustomerReview} and {@link CriticReview}); they share
 * the {@code review} table (SINGLE_TABLE inheritance) and are told apart by the {@code kind} column.
 * The relationship with {@link Product} is owned HERE: the foreign key {@code product_id} lives in this table.
 */
@Entity
@Table(name = "review")
// TODO task 3: make the two kinds share the review table, told apart by a column named kind
//   (values CUSTOMER and CRITIC, see the @JsonSubTypes names below and DiscriminatorValue in the subclasses).
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CustomerReview.class, name = "CUSTOMER"),
        @JsonSubTypes.Type(value = CriticReview.class, name = "CRITIC")
})
public abstract class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    // TODO task 2: this is the OWNING side of the many-to-one: the foreign key column is product_id,
    //   a review cannot exist without a product, and there is no reason to load the product eagerly.
    @Transient
    @JsonIgnore
    public Product product;

    @Column(nullable = false)
    public int rating;

    @Column(nullable = false, length = 500)
    public String comment;

    @Column(nullable = false)
    public Instant createdAt;
}
