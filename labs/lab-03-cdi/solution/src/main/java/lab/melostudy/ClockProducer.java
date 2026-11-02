package lab.melostudy;

import java.time.Clock;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Producer del Clock de producción. {@code @DefaultBean} permite que cualquier otro bean
 * de tipo Clock (por ejemplo, uno fijo en tests) lo sustituya sin ambigüedad.
 */
@ApplicationScoped
public class ClockProducer {

    @Produces
    @Singleton
    @DefaultBean
    Clock clock() {
        return Clock.systemUTC();
    }
}
