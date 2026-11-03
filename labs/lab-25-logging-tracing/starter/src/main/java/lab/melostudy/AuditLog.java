package lab.melostudy;

import java.util.concurrent.CompletableFuture;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

/** Writes the audit trail off the request thread, so the caller does not wait for it. */
@ApplicationScoped
public class AuditLog {

    private static final Logger LOG = Logger.getLogger(AuditLog.class);

    public void orderCreated(Order order) {
        CompletableFuture.runAsync(() -> LOG.infof("Audit: order %d created", order.id()));
    }
}
