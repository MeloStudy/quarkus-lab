package lab.melostudy;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Readiness answers: "should traffic be sent to me right now?" Without the inventory we cannot take orders, so we
 * ask to be taken out of rotation, but we do not ask to be restarted.
 */
@Readiness
@ApplicationScoped
public class InventoryReadinessCheck implements HealthCheck {

    @Inject
    InventoryGateway inventory;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.builder()
                .name("inventory")
                .status(inventory.isAvailable())
                .build();
    }
}
