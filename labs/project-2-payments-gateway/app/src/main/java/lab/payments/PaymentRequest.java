package lab.payments;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What a customer may ask for. The amount is in minor units (cents). The card is a token that the provider's own
 * form produced: the raw card number never reaches this service, and the pattern refuses it if it tries.
 */
public record PaymentRequest(
        @Min(1) @Max(10_000_000) long amount,
        @NotNull @Pattern(regexp = "EUR|USD|PEN") String currency,
        @NotNull @Pattern(regexp = "tok_[A-Za-z0-9]{8,64}") String cardToken,
        @Size(max = 140) String description) {

    /** Identifies the content of the request, so that a reused idempotency key with different content is detected. */
    String fingerprint() {
        return amount + "|" + currency + "|" + cardToken + "|" + (description == null ? "" : description);
    }
}
