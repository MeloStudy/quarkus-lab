package lab.payments;

import io.smallrye.config.ConfigMapping;
import jakarta.validation.constraints.NotBlank;

/**
 * The credentials of the provider. There is no default in {@code application.properties}: a production start without
 * {@code PAYMENTS_PROVIDER_API_KEY} fails at startup, not at the first customer's payment (lab 02).
 * <p>
 * Never print this object: a mapping's {@code toString} shows its values.
 */
@ConfigMapping(prefix = "payments.provider")
public interface ProviderSettings {

    @NotBlank
    String apiKey();
}
