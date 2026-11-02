package lab.melostudy;

import java.math.BigDecimal;
import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/** One conversion that was carried out: the history. */
@Entity
public class Conversion extends PanacheEntity {

    @Column(nullable = false, length = 3)
    public String fromCode;

    @Column(nullable = false, length = 3)
    public String toCode;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal result;

    @Column(nullable = false)
    public Instant createdAt;
}
