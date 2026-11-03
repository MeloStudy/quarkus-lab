package lab.payments;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/** What the API says about a payment. Never the customer, the key, the fingerprint or the card. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        String id,
        PaymentStatus status,
        long amount,
        String currency,
        String description,
        String providerReference,
        String declineReason,
        Instant createdAt,
        Instant updatedAt) {

    static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.id(), p.status(), p.amount(), p.currency(), p.description(), p.providerReference(),
                p.declineReason(), p.createdAt(), p.updatedAt());
    }
}
