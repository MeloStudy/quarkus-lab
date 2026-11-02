package lab.melostudy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

/**
 * Converts catalog prices (stored in USD) using a rate fetched from the remote service.
 * This is where "the remote world" is translated into our domain: callers only ever see
 * {@link Optional}, {@link UnknownCurrencyException}, {@link RatesUnavailableException}
 * or {@link RatesTimeoutException}.
 */
@ApplicationScoped
public class PricingService {

    private final ProductRepository repository;
    private final RateClient rates;

    PricingService(ProductRepository repository, @RestClient RateClient rates) {
        this.repository = repository;
        this.rates = rates;
    }

    /** Empty when the product does not exist (checked BEFORE calling the remote service). */
    public Optional<PriceQuote> quote(long productId, String currency) {
        Optional<Product> product = repository.findOne(productId);
        if (product.isEmpty()) {
            return Optional.empty();
        }
        Rate rate = fetchRate(currency);
        if (rate == null || rate.rate() == null || rate.rate().signum() <= 0) {
            throw new RatesUnavailableException("The rates service sent an unusable rate", null);
        }
        BigDecimal amount = product.get().price.multiply(rate.rate()).setScale(2, RoundingMode.HALF_UP);
        return Optional.of(new PriceQuote(productId, currency, rate.rate(), amount));
    }

    private Rate fetchRate(String currency) {
        try {
            return rates.get(currency);
        } catch (WebApplicationException e) {
            // The remote answered, but the client could not use the answer (e.g. a 200 whose body is not JSON).
            // Never let this reach the generic WebApplicationException mapper: it would blame OUR caller
            // (400 MALFORMED_REQUEST) for the remote's broken body.
            throw new RatesUnavailableException("The rates service sent an unreadable answer", e);
        } catch (ProcessingException e) {
            // Network-level failures (connection refused, timeout, unreadable body) are not HTTP responses,
            // so the @ClientExceptionMapper never sees them.
            if (isTimeout(e)) {
                throw new RatesTimeoutException(e);
            }
            throw new RatesUnavailableException("The rates service could not be reached", e);
        }
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof TimeoutException || t instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
