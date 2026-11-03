package lab.melostudy;

import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/** Records what happened. Recording is asynchronous (it stands for a call to an audit service): it returns a {@code Uni}. */
@ApplicationScoped
public class AuditTrail {

    private final AtomicInteger events = new AtomicInteger();

    public Uni<Void> record(String event) {
        return Uni.createFrom().voidItem().invoke(() -> events.incrementAndGet());
    }

    public int count() {
        return events.get();
    }

    public void reset() {
        events.set(0);
    }
}
