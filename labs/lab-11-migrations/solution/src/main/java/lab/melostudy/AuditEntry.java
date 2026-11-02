package lab.melostudy;

import java.time.Instant;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** One line of the audit trail: what was attempted and how it ended, even when the attempt was rolled back. */
@Entity
@Table(name = "audit_entry")
public class AuditEntry extends PanacheEntity {

    @Column(nullable = false, length = 40)
    public String outcome;

    @Column(nullable = false)
    public Long productId;

    @Column(nullable = false)
    public Instant createdAt;

    public static List<AuditEntry> forProduct(long productId) {
        return list("productId", Sort.by("id"), productId);
    }
}
