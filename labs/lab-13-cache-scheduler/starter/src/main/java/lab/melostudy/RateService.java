package lab.melostudy;

import java.math.BigDecimal;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Exchange rates in front of the slow {@link RateProvider}.
 * TODO (lab 13): today every call pays the upstream latency. Put a cache called {@code rates} in front of it.
 */
@ApplicationScoped
public class RateService {

    private final RateProvider provider;

    RateService(RateProvider provider) {
        this.provider = provider;
    }

    /** TODO task 1: the first call per currency should pay the upstream latency, the next ones should not. */
    public BigDecimal rateOf(String currency) {
        return provider.fetch(currency);
    }

    /** Calls {@link #rateOf} on {@code this}. */
    public BigDecimal quote(String currency, int amount) {
        return rateOf(currency).multiply(BigDecimal.valueOf(amount));
    }

    /** TODO task 2: change a rate upstream and make sure nobody keeps reading the old one. */
    public void change(String currency, BigDecimal newRate) {
        provider.update(currency, newRate);
    }

    /** TODO task 2: drop every cached rate. */
    public void forgetAll() {
    }
}
