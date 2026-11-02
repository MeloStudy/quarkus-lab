package lab.melostudy;

import java.math.BigDecimal;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Stands for a REMOTE fraud-screening service. In this lab it is never reachable: a test that does not replace it
 * gets {@link FraudServiceUnavailableException}, exactly as it would on a machine without network.
 */
@ApplicationScoped
public class FraudClient {

    public boolean isSuspicious(String customerId, BigDecimal amount) {
        throw new FraudServiceUnavailableException();
    }
}
