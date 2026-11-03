package lab.melostudy;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Startup answers: "has the slow initialization finished?" Until it says UP, the platform does not run the other probes. */
@Startup
@ApplicationScoped
public class WorkerStartupCheck implements HealthCheck {

    @Inject
    OrderProcessor processor;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.builder().name("worker-started").status(processor.isRunning()).build();
    }
}
