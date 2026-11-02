package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Nobody injects it: another module finds it by programmatic lookup
 * ({@code Arc.container().instance(...)}).
 */
@ApplicationScoped
public class LegacyExporter {

    public String export() {
        return "csv";
    }
}
