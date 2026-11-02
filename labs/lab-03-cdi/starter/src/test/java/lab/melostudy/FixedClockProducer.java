package lab.melostudy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Test infrastructure (not part of the task): replaces the production Clock
 * with a fixed one. An {@code @Alternative} with {@code @Priority} beats any other bean.
 */
@ApplicationScoped
public class FixedClockProducer {

    public static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Produces
    @Alternative
    @Priority(1)
    @Singleton
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
