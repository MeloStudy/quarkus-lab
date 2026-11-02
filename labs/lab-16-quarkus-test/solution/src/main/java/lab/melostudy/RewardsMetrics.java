package lab.melostudy;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

/** Counts the awards since the application started. In-memory state: it lives as long as the application. */
@ApplicationScoped
public class RewardsMetrics {

    private final AtomicInteger awarded = new AtomicInteger();

    public void record() {
        awarded.incrementAndGet();
    }

    public int awarded() {
        return awarded.get();
    }

    public void reset() {
        awarded.set(0);
    }
}
