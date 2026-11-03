package lab.melostudy;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Liveness answers one question: "is this process stuck in a way that only a restart can fix?" It looks only
 * at the application itself, never at its dependencies: a restart does not repair somebody else's outage.
 */
@Liveness
@ApplicationScoped
public class WorkerLivenessCheck implements HealthCheck {

    @Inject
    OrderProcessor processor;

    @ConfigProperty(name = "app.worker.max-silence-millis", defaultValue = "10000")
    long maxSilenceMillis;

    @Override
    public HealthCheckResponse call() {
        long silence = processor.millisSinceHeartbeat();
        return HealthCheckResponse.builder()
                .name("order-worker")
                .status(silence < maxSilenceMillis)
                .withData("silenceMillis", silence)
                .build();
    }
}
