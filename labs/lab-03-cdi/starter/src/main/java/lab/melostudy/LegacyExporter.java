package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Nadie lo inyecta: lo localiza otro módulo por búsqueda programática
 * ({@code Arc.container().instance(...)}).
 */
@ApplicationScoped
public class LegacyExporter {

    public String export() {
        return "csv";
    }
}
