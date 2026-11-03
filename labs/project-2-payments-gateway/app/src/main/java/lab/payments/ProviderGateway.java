package lab.payments;

import java.net.ConnectException;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The only door to the provider, and where the resilience lives (lab 23).
 * <p>
 * <b>Why retrying a charge is safe here:</b> every attempt carries the same {@code Idempotency-Key}, and the provider
 * charges once per key. Without that, the retry below would be a way of charging customers twice.
 * <p>
 * The breaker looks at the last 20 <i>attempts</i> (a payment that is retried twice is three attempts) and opens when half of them
 * failed; a window of 4 would let one bad payment open it. The tests shrink the window by configuration.
 * <p>
 * What counts as the provider's failure ({@code failOn}, {@code retryOn}): it is slow, unreachable, answers 5xx/429 or
 * garbage. What does not: a decline (402), a rejection (4xx) or a missing record (404). Those are answers, the provider is
 * well; retrying them cannot help and counting them would open the breaker because customers have bad cards.
 */
@ApplicationScoped
public class ProviderGateway {

    public static final String BREAKER = "provider";

    @Inject
    @RestClient
    ProviderClient client;

    @Retry(maxRetries = 2, delay = 100, delayUnit = ChronoUnit.MILLIS, jitter = 0,
            retryOn = { ProviderException.Transient.class, ProviderException.Unreachable.class, TimeoutException.class })
    @CircuitBreaker(requestVolumeThreshold = 20, failureRatio = 0.5, delay = 10, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 1,
            failOn = { ProviderException.Transient.class, ProviderException.Unreachable.class, TimeoutException.class })
    @CircuitBreakerName(BREAKER)
    @Timeout(value = 2000, unit = ChronoUnit.MILLIS)
    public ProviderClient.ChargeAnswer charge(ChargeContext context, String idempotencyKey, ProviderClient.ChargeCommand command) {
        context.attempting();
        try {
            return translated(() -> client.charge(idempotencyKey, command));
        } catch (ProviderException.Unreachable e) {
            context.refused();
            throw e;
        }
    }

    /** Reading is always safe to repeat. */
    @Retry(maxRetries = 2, delay = 100, delayUnit = ChronoUnit.MILLIS, jitter = 0,
            retryOn = { ProviderException.Transient.class, ProviderException.Unreachable.class, TimeoutException.class })
    @Timeout(value = 2000, unit = ChronoUnit.MILLIS)
    public ProviderClient.ChargeAnswer lookup(String idempotencyKey) {
        return translated(() -> client.lookup(idempotencyKey));
    }

    /** Safe to repeat for the same reason as {@link #charge}: the provider refunds once per key. */
    @Retry(maxRetries = 2, delay = 100, delayUnit = ChronoUnit.MILLIS, jitter = 0,
            retryOn = { ProviderException.Transient.class, ProviderException.Unreachable.class, TimeoutException.class })
    @Timeout(value = 2000, unit = ChronoUnit.MILLIS)
    public ProviderClient.ChargeAnswer refund(String chargeId, String idempotencyKey) {
        return translated(() -> client.refund(chargeId, idempotencyKey));
    }

    /**
     * The client reports its own failures as {@code ProcessingException} (timeout, closed connection, refused connection) and
     * {@code ClientWebApplicationException} (a 2xx we cannot read). Only a refused connection proves that nothing was sent.
     */
    private static <T> T translated(Supplier<T> call) {
        try {
            return call.get();
        } catch (ProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
                if (cause instanceof ConnectException) {
                    throw new ProviderException.Unreachable(e);
                }
            }
            throw new ProviderException.Transient("The provider failed: " + e.getClass().getSimpleName(), e);
        }
    }
}
