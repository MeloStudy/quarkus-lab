package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * No Quarkus here: a plain JUnit test that builds the service by hand. No container, no database, no HTTP; it runs
 * in milliseconds. It is possible because every collaborator comes through the constructor.
 */
class RewardsServiceTest {

    private static final Clock MONDAY = Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC);
    private static final Clock SATURDAY = Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC);
    private static final Clock SUNDAY = Clock.fixed(Instant.parse("2026-11-08T10:00:00Z"), ZoneOffset.UTC);

    private FraudClient fraud;
    private Notifier notifier;
    private AccountRepository accounts;
    private RewardsMetrics metrics;

    @BeforeEach
    void setUp() {
        // TODO task 4: no Quarkus here. The three collaborators of the service must be plain Mockito mocks.
        fraud = null;
        notifier = null;
        accounts = null;
        metrics = new RewardsMetrics(); // a real, trivial object needs no mock
        // TODO task 4: by default no customer has an account yet
    }

    private RewardsService on(Clock clock) {
        return new RewardsService(fraud, notifier, accounts, clock, metrics);
    }

    @Test
    void pointsAreOnePerTenUnitsRoundedDown() {
        RewardsService service = on(MONDAY);

        assertEquals(0, service.award("a", new BigDecimal("9.99")).points());
        assertEquals(1, service.award("b", new BigDecimal("10")).points());
        assertEquals(12, service.award("c", new BigDecimal("125")).points());
        assertEquals(0, service.award("d", new BigDecimal("0.01")).points());
    }

    @Test
    void weekendsGiveDoublePoints() {
        assertEquals(24, on(SATURDAY).award("a", new BigDecimal("125")).points());
        assertEquals(24, on(SUNDAY).award("b", new BigDecimal("125")).points());
        assertEquals(12, on(MONDAY).award("c", new BigDecimal("125")).points());
    }

    @Test
    void aSuspiciousOrderStopsEverything() {
        when(fraud.isSuspicious("mallory", new BigDecimal("500"))).thenReturn(true);

        assertThrows(SuspiciousOrderException.class, () -> on(MONDAY).award("mallory", new BigDecimal("500")));

        verifyNoInteractions(accounts, notifier);
        assertEquals(0, metrics.awarded());
    }

    @Test
    void aNewCustomerGetsANewAccount() {
        RewardsService.Award award = on(MONDAY).award("ana", new BigDecimal("125"));

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accounts).persist(saved.capture());
        assertEquals("ana", saved.getValue().customerId);
        assertEquals(12, saved.getValue().points);
        assertEquals(12, award.balance());
    }

    @Test
    void anExistingAccountIsIncrementedNotReplaced() {
        when(accounts.findByCustomer("ana")).thenReturn(Optional.of(new Account("ana", 10)));

        RewardsService.Award award = on(MONDAY).award("ana", new BigDecimal("55"));

        assertEquals(15, award.balance());
        verify(accounts, never()).persist(any(Account.class));
    }

    @Test
    void theCustomerIsToldHowManyPointsTheyEarned() {
        on(MONDAY).award("ana", new BigDecimal("125"));

        verify(notifier).send("ana", "You earned 12 points");
        assertEquals(1, metrics.awarded());
    }

    @Test
    void aFailingNotificationDoesNotUndoTheAward() {
        doThrow(new RuntimeException("smtp is down")).when(notifier).send(anyString(), anyString());

        RewardsService.Award award = on(MONDAY).award("ana", new BigDecimal("125"));

        assertEquals(12, award.balance());
        assertEquals(1, metrics.awarded());
    }

    @Test
    void invalidInputNeverReachesTheCollaborators() {
        RewardsService service = on(MONDAY);

        assertThrows(IllegalArgumentException.class, () -> service.award("ana", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> service.award("ana", new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> service.award("ana", null));
        assertThrows(IllegalArgumentException.class, () -> service.award(" ", BigDecimal.TEN));

        verifyNoInteractions(fraud, accounts, notifier);
    }
}
