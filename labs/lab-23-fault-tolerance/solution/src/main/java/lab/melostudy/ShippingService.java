package lab.melostudy;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Asks the carrier for a price. The carrier is a remote service: it will be slow, it will fail, it will fail for a while. */
@ApplicationScoped
public class ShippingService {

    public static final String BREAKER = "carrier";

    @Inject
    @RestClient
    CarrierClient carrier;

    private final Map<String, Double> lastKnown = new ConcurrentHashMap<>();

    @Fallback(fallbackMethod = "fromCache", skipOn = UnknownCityException.class)
    @Retry(maxRetries = 2, delay = 100, delayUnit = ChronoUnit.MILLIS, jitter = 0,
            retryOn = { CarrierUnavailableException.class, TimeoutException.class })
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 10, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 1, failOn = { CarrierUnavailableException.class, TimeoutException.class })
    @CircuitBreakerName(BREAKER)
    @Timeout(value = 500, unit = ChronoUnit.MILLIS)
    @Bulkhead(2)
    public Quote quote(String city) {
        CarrierClient.CarrierQuote answer = carrier.quote(city);
        lastKnown.put(city, answer.price());
        return new Quote(answer.city(), answer.price(), "carrier");
    }

    /** The carrier cannot help right now: the last price we saw is better than an error, but only if we saw one. */
    Quote fromCache(String city) {
        Double price = lastKnown.get(city);
        if (price == null) {
            throw new ShippingUnavailableException("The carrier is not available and there is no previous price for " + city);
        }
        return new Quote(city, price, "cache");
    }

    /** Only the tests call this, to start every test from an empty cache. */
    public void clearCache() {
        lastKnown.clear();
    }
}
