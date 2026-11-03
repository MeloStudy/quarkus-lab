package lab.melostudy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import jakarta.inject.Inject;

/** Every test starts with a healthy inventory and a worker that works. */
abstract class OpsTestBase {

    @Inject
    InventoryGateway inventory;

    @Inject
    OrderProcessor processor;

    @BeforeEach
    @AfterEach
    void everythingWorks() {
        inventory.setAvailable(true);
        processor.stall(false);
    }
}
