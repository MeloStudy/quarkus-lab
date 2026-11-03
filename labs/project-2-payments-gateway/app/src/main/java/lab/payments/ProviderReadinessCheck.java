package lab.payments;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Reports the state of the provider breaker, but <b>stays UP</b> when it is open. Readiness decides whether this instance
 * receives traffic; every instance would say DOWN at the same moment, the whole service would be taken out of rotation, and
 * reading payments (which does not need the provider) would stop too. A provider outage must degrade the writes only.
 * The state is data for the operator; the alert is on the metric.
 */
@Readiness
@ApplicationScoped
public class ProviderReadinessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        CircuitBreakerState state = CircuitBreakerMaintenance.get().currentState(ProviderGateway.BREAKER);
        return HealthCheckResponse.builder().name("provider").up().withData("circuit", state.name()).build();
    }
}
