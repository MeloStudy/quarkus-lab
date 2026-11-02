package lab.melostudy;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/** The application asks for a {@link Clock} instead of calling {@code Instant.now()}: that is what makes time testable. */
public class ClockProducer {

    @Produces
    @ApplicationScoped
    Clock clock() {
        return Clock.systemUTC();
    }
}
