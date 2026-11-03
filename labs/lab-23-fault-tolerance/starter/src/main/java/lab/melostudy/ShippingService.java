package lab.melostudy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.rest.client.inject.RestClient;

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

    // TODO task 1: give up after 500 ms
    // TODO task 2: retry transient failures (2 retries, 100 ms apart), but not the caller's mistakes
    // TODO task 3: when everything fails, answer with the last known price (write the fallback method below)
    // TODO task 4: stop calling a carrier that keeps failing (name the breaker ShippingService.BREAKER)
    // TODO task 5: at most 2 requests inside at the same time
    public Quote quote(String city) {
        CarrierClient.CarrierQuote answer = carrier.quote(city);
        lastKnown.put(city, answer.price());
        return new Quote(answer.city(), answer.price(), "carrier");
    }

    /** Only the tests call this, to start every test from an empty cache. */
    public void clearCache() {
        lastKnown.clear();
    }
}
