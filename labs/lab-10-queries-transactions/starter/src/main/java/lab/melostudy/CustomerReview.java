package lab.melostudy;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** A review written by a customer. */
@Entity
@DiscriminatorValue("CUSTOMER")
public class CustomerReview extends Review {

    /** Column {@code verified_purchase}: it exists for ALL rows of the shared table, so it must be nullable. */
    public Boolean verifiedPurchase;
}
