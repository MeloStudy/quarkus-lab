package lab.clinic;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Reports the state of the reminders and of the provider breaker, but <b>stays UP</b> whatever they say. Readiness decides
 * whether this instance receives traffic; if every instance said DOWN because the SMS provider is down, the whole clinic would be
 * taken out of rotation and nobody could book an appointment - which does not need the provider at all. A provider outage must
 * degrade the reminders only. The numbers are data for the operator; the alert is on the metrics.
 */
@Readiness
@ApplicationScoped
public class ReminderReadinessCheck implements HealthCheck {

    private final ReminderStore store;

    ReminderReadinessCheck(ReminderStore store) {
        this.store = store;
    }

    @Override
    public HealthCheckResponse call() {
        CircuitBreakerState circuit = CircuitBreakerMaintenance.get().currentState(ReminderGateway.BREAKER);
        return HealthCheckResponse.builder().name("reminders").up().withData("circuit", circuit.name())
                .withData("pending", store.count(ReminderStatus.PENDING)).withData("failed", store.count(ReminderStatus.FAILED))
                .withData("oldestOverdueSeconds", store.oldestOverdue().toSeconds()).build();
    }
}
