package lab.pricing;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

/** How many calls to {@link Logged} methods the application has seen. */
@ApplicationScoped
public class CallCounter {

    private final AtomicInteger calls = new AtomicInteger();

    public void hit() {
        calls.incrementAndGet();
    }

    public int calls() {
        return calls.get();
    }
}
