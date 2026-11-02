package lab.melostudy;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/** The loyalty points of one customer. */
@Entity
public class Account extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 60)
    public String customerId;

    public int points;

    public Account() {
    }

    public Account(String customerId, int points) {
        this.customerId = customerId;
        this.points = points;
    }
}
