package lab.melostudy;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

public class MetricsConfig {

    /** Every metric of this application carries {@code application="orders"}: the Prometheus server scrapes many applications. */
    @Produces
    @Singleton
    public MeterFilter commonTags() {
        return MeterFilter.commonTags(Tags.of("application", "orders"));
    }
}
