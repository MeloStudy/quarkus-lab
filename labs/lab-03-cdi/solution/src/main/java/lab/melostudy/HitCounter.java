package lab.melostudy;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Hit counter. {@code HitWriter} writes and {@code HitReader} reads: both
 * must see the same counter.
 */
@ApplicationScoped
public class HitCounter {

    private final AtomicInteger value = new AtomicInteger();

    public void increment() {
        value.incrementAndGet();
    }

    public int value() {
        return value.get();
    }
}
