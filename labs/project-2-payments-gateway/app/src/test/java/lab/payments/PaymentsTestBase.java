package lab.payments;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.test.common.QuarkusTestResource;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;

/** Every test starts with no payments, a healthy provider that has seen nothing, and a closed circuit breaker. */
@QuarkusTestResource(ProviderStub.class)
abstract class PaymentsTestBase {

    @Inject
    PaymentStore store;

    /**
     * Makes payments fail until the breaker opens, and returns how many of them were recorded (as UNKNOWN). It does not assume
     * how many that takes: every attempt counts in the rolling window of four, a payment that succeeded earlier included.
     */
    int openTheBreaker() {
        String ana = Tokens.customer(Api.ANA);
        Provider.failNextCharges(100, 503);
        int recorded = 0;
        for (int i = 0; i < 6; i++) {
            int status = Api.pay(ana, "open-" + i, Api.body(Api.CARD)).statusCode();
            if (status == 503) {
                return recorded;
            }
            org.junit.jupiter.api.Assertions.assertEquals(202, status, "a failing provider leaves the payment UNKNOWN");
            recorded++;
        }
        throw new AssertionError("the breaker did not open after six failing payments");
    }

    @BeforeEach
    void startClean() {
        store.clear();
        Provider.reset();
        CircuitBreakerMaintenance.get().resetAll();
    }
}
