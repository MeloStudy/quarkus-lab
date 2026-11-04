package lab.clinic;

import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

/**
 * The one place that talks to the provider, and the one place that decides what a failure means (labs 7 and 23).
 *
 * <ul>
 *   <li>A 4xx is the provider saying "never": {@link PermanentFailure}. It is not retried and it does not count against the breaker.</li>
 *   <li>A 5xx, a 429, a timeout, a refused or dropped connection is "not now": {@link TransientFailure}. It is retried a
 *       couple of times at once (same idempotency key) and counts against the breaker.</li>
 * </ul>
 *
 * Every policy names its exceptions: the defaults of fault tolerance react to ANY exception, so a rejected address would
 * be retried and would open the breaker for everybody else.
 */
@ApplicationScoped
public class ReminderGateway {

    /** The name under which the breaker is known to the readiness check and to the operator. */
    public static final String BREAKER = "notifications";

    private final NotificationClient client;

    ReminderGateway(@RestClient NotificationClient client, NotificationConfig credentials) {
        // credentials is injected only so that a missing key stops the startup; the client reads it from the configuration
        this.client = client;
    }

    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = { TransientFailure.class, TimeoutException.class },
            abortOn = PermanentFailure.class)
    @CircuitBreakerName(BREAKER)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 10, delayUnit = ChronoUnit.SECONDS,
            failOn = { TransientFailure.class, TimeoutException.class }, skipOn = PermanentFailure.class)
    public void send(String idempotencyKey, String to, String text) {
        try {
            client.send(idempotencyKey, new NotificationClient.Message(to, text)).close();
        } catch (WebApplicationException e) {
            int status = e.getResponse().getStatus();
            if (status == 429 || status >= 500) {
                throw new TransientFailure("the provider answered " + status, e);
            }
            throw new PermanentFailure("the provider refused the message with " + status);
        } catch (ProcessingException e) {
            throw new TransientFailure("the provider could not be reached: " + e.getClass().getSimpleName(), e);
        }
    }
}
