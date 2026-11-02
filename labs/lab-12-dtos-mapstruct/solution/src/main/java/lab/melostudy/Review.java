package lab.melostudy;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A review of a product. There are two kinds ({@link CustomerReview} and {@link CriticReview}); they share
 * the {@code review} table (SINGLE_TABLE inheritance) and are told apart by the {@code kind} column.
 * The relationship with {@link Product} is owned HERE: the foreign key {@code product_id} lives in this table.
 */
@Entity
@Table(name = "review")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kind", discriminatorType = DiscriminatorType.STRING, length = 10)
public abstract class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    public Product product;

    @Column(nullable = false)
    public int rating;

    @Column(nullable = false, length = 500)
    public String comment;

    @Column(nullable = false)
    public Instant createdAt;
}
