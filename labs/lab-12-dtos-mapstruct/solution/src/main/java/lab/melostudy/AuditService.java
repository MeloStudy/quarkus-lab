package lab.melostudy;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/**
 * Writes the audit trail. {@code REQUIRES_NEW} suspends the caller's transaction and runs in its own, so
 * the entry is committed even if the caller rolls back afterwards.
 */
@ApplicationScoped
public class AuditService {

    @Transactional(TxType.REQUIRES_NEW)
    public void record(String outcome, long productId) {
        AuditEntry entry = new AuditEntry();
        entry.outcome = outcome;
        entry.productId = productId;
        entry.createdAt = Instant.now();
        entry.persist();
    }
}
