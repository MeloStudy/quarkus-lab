package lab.melostudy;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;

/**
 * Hit counter. {@code HitWriter} writes and {@code HitReader} reads: both
 * must see the same counter.
 */
// TODO (Task 1): is @Dependent the right scope for shared state?
@Dependent
public class HitCounter {

    private final AtomicInteger value = new AtomicInteger();

    public void increment() {
        value.incrementAndGet();
    }

    public int value() {
        return value.get();
    }
}
