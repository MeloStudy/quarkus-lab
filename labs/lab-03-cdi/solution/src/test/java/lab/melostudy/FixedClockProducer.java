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
 * Infraestructura de test (no es parte de la tarea): sustituye el Clock de producción
 * por uno fijo. Un {@code @Alternative} con {@code @Priority} gana a cualquier otro bean.
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
