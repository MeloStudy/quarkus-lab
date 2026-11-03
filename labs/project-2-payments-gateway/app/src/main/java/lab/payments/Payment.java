package lab.payments;

import java.time.Instant;

/**
 * One payment. It never holds the card token: the token is needed once, to ask the provider, and not kept.
 * {@code fingerprint} identifies the content of the request that created it (see the idempotency rules).
 */
public record Payment(
        String id,
        String customer,
        String idempotencyKey,
        String fingerprint,
        long amount,
        String currency,
        String description,
        PaymentStatus status,
        String providerReference,
        String declineReason,
        Instant createdAt,
        Instant updatedAt) {

    public Payment settled(PaymentStatus newStatus, String reference, String reason, Instant now) {
        return new Payment(id, customer, idempotencyKey, fingerprint, amount, currency, description, newStatus,
                reference != null ? reference : providerReference, reason != null ? reason : declineReason, createdAt, now);
    }
}
