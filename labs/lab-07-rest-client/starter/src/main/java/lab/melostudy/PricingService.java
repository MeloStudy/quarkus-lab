package lab.melostudy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Converts catalog prices (stored in USD) using a rate fetched from the remote service.
 * This is where "the remote world" is translated into our domain: callers only ever see
 * {@link Optional}, {@link UnknownCurrencyException}, {@link RatesUnavailableException}
 * or {@link RatesTimeoutException}.
 */
@ApplicationScoped
public class PricingService {

    private final ProductRepository repository;

    // TODO (task 1): also receive the REST client here (constructor injection, with the right qualifier)
    PricingService(ProductRepository repository) {
        this.repository = repository;
    }

    /** Empty when the product does not exist (checked BEFORE calling the remote service). */
    public Optional<PriceQuote> quote(long productId, String currency) {
        Optional<Product> product = repository.findById(productId);
        if (product.isEmpty()) {
            return Optional.empty();
        }
        Rate rate = fetchRate(currency);
        if (rate == null || rate.rate() == null || rate.rate().signum() <= 0) {
            throw new RatesUnavailableException("The rates service sent an unusable rate", null);
        }
        BigDecimal amount = product.get().price().multiply(rate.rate()).setScale(2, RoundingMode.HALF_UP);
        return Optional.of(new PriceQuote(productId, currency, rate.rate(), amount));
    }

    /**
     * TODO (tasks 1-4): call the remote service and translate EVERYTHING that can go wrong into
     * UnknownCurrencyException, RatesUnavailableException or RatesTimeoutException.
     */
    private Rate fetchRate(String currency) {
        throw new UnsupportedOperationException("TODO: call the remote exchange-rate service");
    }
}
