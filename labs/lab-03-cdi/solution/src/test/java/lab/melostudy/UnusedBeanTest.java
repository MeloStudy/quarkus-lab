package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class UnusedBeanTest {

    @Test
    void legacyExporterIsAvailableByProgrammaticLookup() {
        InstanceHandle<LegacyExporter> handle = Arc.container().instance(LegacyExporter.class);
        assertTrue(handle.isAvailable(), "ArC removed the bean because nobody injects it");
        assertEquals("csv", handle.get().export());
    }
}
