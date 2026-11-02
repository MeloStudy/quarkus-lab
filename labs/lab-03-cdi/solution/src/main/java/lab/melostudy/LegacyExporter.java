package lab.melostudy;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Nadie lo inyecta: lo localiza otro módulo por búsqueda programática
 * ({@code Arc.container().instance(...)}).
 */
@Unremovable
@ApplicationScoped
public class LegacyExporter {

    public String export() {
        return "csv";
    }
}
