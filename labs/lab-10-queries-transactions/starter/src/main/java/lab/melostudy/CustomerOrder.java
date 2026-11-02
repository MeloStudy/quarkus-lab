package lab.melostudy;

import java.math.BigDecimal;
import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** An accepted order. (The table cannot be called {@code order}: it is an SQL keyword.) */
@Entity
@Table(name = "customer_order")
public class CustomerOrder extends PanacheEntity {

    @Column(nullable = false)
    public Long productId;

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false, precision = 11, scale = 2)
    public BigDecimal total;

    @Column(nullable = false)
    public Instant createdAt;
}
