package lab.melostudy;

import java.time.Clock;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Production Clock producer. {@code @DefaultBean} lets any other bean
 * of type Clock (for example, a fixed one in tests) replace it without ambiguity.
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
