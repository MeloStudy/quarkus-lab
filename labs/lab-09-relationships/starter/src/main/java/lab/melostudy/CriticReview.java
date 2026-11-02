package lab.melostudy;

import jakarta.persistence.Entity;

/** A review written by a critic, published in some outlet. */
@Entity
// TODO task 3: the discriminator value of this kind is "CRITIC"
public class CriticReview extends Review {

    /** Column {@code outlet}: null for customer rows, because the table is shared. */
    public String outlet;
}
