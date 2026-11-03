package lab.melostudy;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

/** What the server remembers about its own work, so that a test can look at the server side of a call. */
@ApplicationScoped
public class ServerStats {

    private volatile String lastGetProductThread;
    private final AtomicInteger streamed = new AtomicInteger();
    private final AtomicInteger streamsCancelled = new AtomicInteger();

    public void getProductRanOn(String thread) {
        lastGetProductThread = thread;
    }

    public String lastGetProductThread() {
        return lastGetProductThread;
    }

    public void productStreamed() {
        streamed.incrementAndGet();
    }

    public void streamCancelled() {
        streamsCancelled.incrementAndGet();
    }

    public int streamed() {
        return streamed.get();
    }

    public int streamsCancelled() {
        return streamsCancelled.get();
    }

    public void reset() {
        lastGetProductThread = null;
        streamed.set(0);
        streamsCancelled.set(0);
    }
}
