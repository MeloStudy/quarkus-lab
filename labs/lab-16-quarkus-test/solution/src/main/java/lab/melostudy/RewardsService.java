package lab.melostudy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Clock;
import java.time.LocalDate;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Gives loyalty points for an order: 1 point per 10 currency units (rounded down), double on Saturday and Sunday,
 * nothing if the fraud service flags the order. Every collaborator comes through the constructor, so the class can
 * be created in a plain unit test with fakes or mocks, without starting Quarkus.
 */
@ApplicationScoped
public class RewardsService {

    private static final Logger LOG = Logger.getLogger(RewardsService.class);

    public record Award(String customerId, int points, int balance) {
    }

    private final FraudClient fraud;
    private final Notifier notifier;
    private final AccountRepository accounts;
    private final Clock clock;
    private final RewardsMetrics metrics;

    RewardsService(FraudClient fraud, Notifier notifier, AccountRepository accounts, Clock clock, RewardsMetrics metrics) {
        this.fraud = fraud;
        this.notifier = notifier;
        this.accounts = accounts;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public Award award(String customerId, BigDecimal amount) {
        if (customerId == null || customerId.isBlank() || amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("a customer and a positive amount are required");
        }
        if (fraud.isSuspicious(customerId, amount)) {
            throw new SuspiciousOrderException(customerId);
        }
        int points = amount.divide(BigDecimal.TEN, 0, RoundingMode.DOWN).intValue();
        DayOfWeek today = LocalDate.now(clock).getDayOfWeek();
        if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) {
            points *= 2;
        }
        Account account = accounts.findByCustomer(customerId).orElseGet(() -> {
            Account created = new Account(customerId, 0);
            accounts.persist(created);
            return created;
        });
        account.points += points;
        metrics.record();
        try {
            notifier.send(customerId, "You earned " + points + " points");
        } catch (RuntimeException e) {
            // a failed notification must never undo the award
            LOG.warnf("notification to %s failed: %s", customerId, e.getMessage());
        }
        return new Award(customerId, points, account.points);
    }

    public int balanceOf(String customerId) {
        return accounts.findByCustomer(customerId).map(a -> a.points).orElse(0);
    }
}
