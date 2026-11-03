package lab.melostudy;

import org.junit.jupiter.api.BeforeEach;

import jakarta.inject.Inject;

/** Every test starts with an empty cache and a driver that has never been called. */
abstract class ExecutionTestBase {

    @Inject
    LegacyCatalog catalog;

    @Inject
    PriceCache cache;

    @BeforeEach
    void clean() {
        cache.clear();
        catalog.resetCalls();
    }
}
