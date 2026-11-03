package lab.payments;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

public class MetricsConfig {

    /** Every metric of this application carries {@code application="payments"}: the Prometheus server scrapes many applications. */
    @Produces
    @Singleton
    public MeterFilter commonTags() {
        return MeterFilter.commonTags(Tags.of("application", "payments"));
    }

    /**
     * How many payments are waiting for a human or a reconciliation. This is the number to alert on: an UNKNOWN payment is a
     * customer who does not know whether they paid. Tags are the outcome kinds, never a payment or customer id.
     */
    @ApplicationScoped
    public static class UnknownPaymentsGauge {

        @Inject
        PaymentStore store;

        void register(@Observes StartupEvent event, MeterRegistry registry) {
            Gauge.builder("payments.unknown", store, s -> s.count(PaymentStatus.UNKNOWN))
                    .description("Payments whose outcome at the provider is not known yet").register(registry);
        }
    }
}
