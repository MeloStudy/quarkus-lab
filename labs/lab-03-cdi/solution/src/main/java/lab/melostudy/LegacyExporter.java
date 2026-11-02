package lab.melostudy;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Nobody injects it: another module finds it by programmatic lookup
 * ({@code Arc.container().instance(...)}).
 */
@Unremovable
@ApplicationScoped
public class LegacyExporter {

    public String export() {
        return "csv";
    }
}
