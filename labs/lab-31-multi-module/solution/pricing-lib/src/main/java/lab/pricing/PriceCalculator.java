package lab.pricing;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/** The pricing rule of the library: quantity times unit price, plus tax. The tax rate is configuration with a default. */
@ApplicationScoped
public class PriceCalculator {

    @ConfigProperty(name = "pricing.tax-rate", defaultValue = "0.20")
    double taxRate;

    @Logged
    public long total(int quantity, long unitCents) {
        return Math.round(quantity * unitCents * (1 + taxRate));
    }
}
