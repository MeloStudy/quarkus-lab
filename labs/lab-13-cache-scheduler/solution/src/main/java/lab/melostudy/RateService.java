package lab.melostudy;

import java.math.BigDecimal;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;

/** Exchange rates in front of the slow {@link RateProvider}. The cache is called {@code rates}; its key is the currency. */
@ApplicationScoped
public class RateService {

    private final RateProvider provider;

    RateService(RateProvider provider) {
        this.provider = provider;
    }

    /** Cached: the first call per currency pays the upstream latency, the next ones do not. */
    @CacheResult(cacheName = "rates")
    public BigDecimal rateOf(String currency) {
        return provider.fetch(currency);
    }

    /** Calls {@link #rateOf} on {@code this}: Quarkus intercepts that call too, so it is served from the cache. */
    public BigDecimal quote(String currency, int amount) {
        return rateOf(currency).multiply(BigDecimal.valueOf(amount));
    }

    /**
     * Changes a rate upstream and drops ONLY that currency from the cache. {@code @CacheKey} marks which
     * parameter is the key: without it the key would be (currency, newRate) and nothing would be invalidated.
     */
    @CacheInvalidate(cacheName = "rates")
    public void change(@CacheKey String currency, BigDecimal newRate) {
        provider.update(currency, newRate);
    }

    /** Drops every cached rate. */
    @CacheInvalidateAll(cacheName = "rates")
    public void forgetAll() {
    }
}
