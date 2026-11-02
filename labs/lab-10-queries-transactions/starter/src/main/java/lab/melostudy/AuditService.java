package lab.melostudy;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Writes the audit trail: one entry per order attempt, with its outcome.
 */
@ApplicationScoped
public class AuditService {

    // TODO task 3: an audit entry must survive when the caller's transaction rolls back.
    @Transactional
    public void record(String outcome, long productId) {
        AuditEntry entry = new AuditEntry();
        entry.outcome = outcome;
        entry.productId = productId;
        entry.createdAt = Instant.now();
        entry.persist();
    }
}
