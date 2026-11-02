package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class OrderEventsTest {

    @Inject
    OrderService orders;

    @Inject
    EventLog log;

    @BeforeEach
    void reset() {
        log.clear();
    }

    @Test
    void synchronousObserversRunBeforePlaceReturnsAndInPriorityOrder() {
        orders.place("webcam", 1);

        List<String> sync = log.entries().stream()
                .filter(e -> !e.startsWith("notify:"))
                .toList();
        assertEquals(List.of("stock:webcam", "shipping:webcam"), sync);
    }

    @Test
    void asynchronousObserverEventuallyRuns() {
        orders.place("hub", 1);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertEquals(true, log.entries().contains("notify:hub")));
    }
}
