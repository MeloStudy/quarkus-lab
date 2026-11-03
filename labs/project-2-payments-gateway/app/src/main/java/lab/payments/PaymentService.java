package lab.payments;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The rules. One question runs through all of them: <b>after this failure, can the customer's money have moved?</b>
 * <ul>
 *   <li>The provider said yes or no: {@code CAPTURED} / {@code DECLINED}.</li>
 *   <li>The provider refused to look at it: {@code FAILED}. Nobody was charged.</li>
 *   <li>We never got to ask (breaker open, connection refused on every attempt): nothing is recorded and the key is free
 *       again. The customer is told to retry, with the same key if they like.</li>
 *   <li>We asked and lost the answer: {@code UNKNOWN}. We do not guess. {@link #reconcile} asks the provider later.</li>
 * </ul>
 */
@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    /** Matches the 10 seconds the breaker stays open in production. */
    static final int RETRY_AFTER_SECONDS = 10;

    /** The payment as it is, and whether it is the answer to an earlier request with the same key. */
    public record Outcome(Payment payment, boolean replayed) {
    }

    @Inject
    PaymentStore store;

    @Inject
    ProviderGateway provider;

    @Inject
    MeterRegistry registry;

    // ---------------------------------------------------------------------------------------------------- create

    @WithSpan("payment.create")
    public Outcome create(String customer, String idempotencyKey, PaymentRequest request) {
        Instant now = Instant.now();
        Payment candidate = new Payment("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20), customer,
                idempotencyKey, request.fingerprint(), request.amount(), request.currency(), request.description(),
                PaymentStatus.PENDING, null, null, now, now);

        PaymentStore.Reservation reservation = store.reserve(candidate);
        if (!reservation.created()) {
            return replay(reservation.payment(), candidate.fingerprint());
        }
        Span.current().setAttribute("payment.id", candidate.id());

        Payment payment = candidate;
        ChargeContext context = new ChargeContext();
        Timer.Sample sample = Timer.start(registry);
        String outcome;
        try {
            ProviderClient.ChargeAnswer answer = provider.charge(context, payment.id(),
                    new ProviderClient.ChargeCommand(request.amount(), request.currency(), request.cardToken()));
            payment = store.update(payment.settled(PaymentStatus.CAPTURED, answer.id(), null, Instant.now()));
            outcome = "captured";
        } catch (ProviderException.Declined e) {
            payment = store.update(payment.settled(PaymentStatus.DECLINED, null, e.reason(), Instant.now()));
            outcome = "declined";
        } catch (ProviderException.Rejected e) {
            // The provider refuses OUR request (bad credentials, bad contract): a problem on our side that must be fixed.
            payment = store.update(payment.settled(PaymentStatus.FAILED, null, "provider_rejected", Instant.now()));
            outcome = "failed";
            LOG.errorf("The provider rejected payment %s with HTTP %d: check our credentials and the contract", payment.id(), e.status());
        } catch (RuntimeException e) {
            if (!context.mayHaveBeenProcessed()) {
                sample.stop(timer("charge", "not_attempted"));
                store.release(payment);
                registry.counter("payments.created", "outcome", "not_attempted").increment();
                LOG.warnf("Payment %s was not sent to the provider (%s); the key is free again", payment.id(), e.getClass().getSimpleName());
                Span.current().setAttribute("payment.outcome", "not_attempted");
                throw new PaymentException(503, "PROVIDER_UNAVAILABLE",
                        "The payment provider is not available right now; nothing was charged, try again later", null, RETRY_AFTER_SECONDS);
            }
            payment = store.update(payment.settled(PaymentStatus.UNKNOWN, null, null, Instant.now()));
            outcome = "unknown";
            LOG.warnf("Payment %s is UNKNOWN: the provider did not answer (%s). Reconcile it before telling anyone it failed",
                    payment.id(), e.getClass().getSimpleName());
        }
        sample.stop(timer("charge", outcome));
        registry.counter("payments.created", "outcome", outcome).increment();
        Span.current().setAttribute("payment.outcome", outcome);
        LOG.infof("Payment %s is %s", payment.id(), payment.status());
        return new Outcome(payment, false);
    }

    private Outcome replay(Payment existing, String fingerprint) {
        if (!existing.fingerprint().equals(fingerprint)) {
            throw new PaymentException(422, "IDEMPOTENCY_KEY_REUSED",
                    "This Idempotency-Key was used for a different payment; use a new key for a new payment");
        }
        if (existing.status() == PaymentStatus.PENDING) {
            throw new PaymentException(409, "REQUEST_IN_PROGRESS",
                    "The first request with this Idempotency-Key is still being processed; ask again in a moment", null, 1);
        }
        registry.counter("payments.replayed").increment();
        LOG.infof("Payment %s replayed for a repeated Idempotency-Key", existing.id());
        return new Outcome(existing, true);
    }

    // ------------------------------------------------------------------------------------------------------ read

    /** A customer sees their own payments; finance sees all. Somebody else's payment does not exist. */
    public Payment get(String id, String customer, boolean finance) {
        return store.find(id).filter(p -> finance || p.customer().equals(customer))
                .orElseThrow(() -> new PaymentException(404, "NOT_FOUND", "No such payment"));
    }

    public List<Payment> list(String customer, boolean finance, PaymentStatus status) {
        return store.list(finance ? null : customer, status);
    }

    // --------------------------------------------------------------------------------------------- reconcile, refund

    /** Settles an {@code UNKNOWN} payment by asking the provider what it really did with that key. */
    @WithSpan("payment.reconcile")
    public Payment reconcile(String id) {
        Payment payment = store.find(id).orElseThrow(() -> new PaymentException(404, "NOT_FOUND", "No such payment"));
        if (payment.status() != PaymentStatus.UNKNOWN) {
            throw new PaymentException(409, "INVALID_STATE", "Only an UNKNOWN payment can be reconciled");
        }
        Span.current().setAttribute("payment.id", id);
        Timer.Sample sample = Timer.start(registry);
        String outcome;
        try {
            ProviderClient.ChargeAnswer answer = provider.lookup(payment.id());
            if ("captured".equals(answer.status())) {
                payment = store.update(payment.settled(PaymentStatus.CAPTURED, answer.id(), null, Instant.now()));
                outcome = "captured";
            } else {
                payment = store.update(payment.settled(PaymentStatus.DECLINED, null, answer.reason(), Instant.now()));
                outcome = "declined";
            }
        } catch (ProviderException.NotFound e) {
            // The provider has no record of the key: it never saw the request, so nobody was charged.
            payment = store.update(payment.settled(PaymentStatus.FAILED, null, "never_reached_provider", Instant.now()));
            outcome = "failed";
        } catch (RuntimeException e) {
            sample.stop(timer("lookup", "unavailable"));
            LOG.warnf("Payment %s stays UNKNOWN: the lookup failed (%s)", id, e.getClass().getSimpleName());
            throw unavailable();
        }
        sample.stop(timer("lookup", outcome));
        registry.counter("payments.reconciled", "outcome", outcome).increment();
        LOG.infof("Payment %s reconciled as %s", payment.id(), payment.status());
        return payment;
    }

    /** Gives a captured payment back. Asking twice is harmless: the provider refunds once per key. */
    @WithSpan("payment.refund")
    public Payment refund(String id) {
        Payment payment = store.find(id).orElseThrow(() -> new PaymentException(404, "NOT_FOUND", "No such payment"));
        if (payment.status() == PaymentStatus.REFUNDED) {
            return payment;
        }
        if (payment.status() != PaymentStatus.CAPTURED) {
            throw new PaymentException(409, "INVALID_STATE", "Only a CAPTURED payment can be refunded");
        }
        Span.current().setAttribute("payment.id", id);
        Timer.Sample sample = Timer.start(registry);
        try {
            provider.refund(payment.providerReference(), "refund-" + payment.id());
        } catch (RuntimeException e) {
            sample.stop(timer("refund", "unavailable"));
            LOG.warnf("Payment %s was not refunded: the provider failed (%s). It is still CAPTURED; asking again is safe",
                    id, e.getClass().getSimpleName());
            throw unavailable();
        }
        payment = store.update(payment.settled(PaymentStatus.REFUNDED, null, null, Instant.now()));
        sample.stop(timer("refund", "refunded"));
        registry.counter("payments.refunded").increment();
        LOG.infof("Payment %s refunded", payment.id());
        return payment;
    }

    // ------------------------------------------------------------------------------------------------------ helpers

    private Timer timer(String operation, String outcome) {
        return Timer.builder("payments.provider.duration").description("Time spent asking the provider, retries included")
                .tag("operation", operation).tag("outcome", outcome).publishPercentileHistogram(false)
                .register(registry);
    }

    private static PaymentException unavailable() {
        return new PaymentException(503, "PROVIDER_UNAVAILABLE",
                "The payment provider is not available right now; try again later", null, RETRY_AFTER_SECONDS);
    }
}
