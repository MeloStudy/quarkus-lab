package lab.melostudy;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Writes the audit trail off the request thread, so the caller does not wait for it. */
@ApplicationScoped
public class AuditLog {

    private static final Logger LOG = Logger.getLogger(AuditLog.class);

    @Inject
    ManagedExecutor executor;

    public void orderCreated(Order order) {
        executor.runAsync(() -> LOG.infof("Audit: order %d created", order.id()));
    }
}
