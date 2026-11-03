package lab.melostudy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * A background worker that processes the orders one by one. It writes a heartbeat every time around its loop,
 * so that somebody from the outside can tell a worker that works from one that is stuck.
 */
@ApplicationScoped
public class OrderProcessor {

    @Inject
    OrderStore store;

    @ConfigProperty(name = "app.worker.work-millis", defaultValue = "20")
    long workMillis;

    private final BlockingQueue<Long> queue = new LinkedBlockingQueue<>();
    private final AtomicLong lastHeartbeat = new AtomicLong(System.currentTimeMillis());
    private volatile boolean stalled;
    private volatile boolean running;
    private Thread worker;

    void onStart(@Observes StartupEvent event) {
        // TODO task 3: expose the size of the queue as a gauge named orders.queue.size
        running = true;
        worker = new Thread(this::loop, "order-worker");
        worker.setDaemon(true);
        worker.start();
    }

    void onStop(@Observes ShutdownEvent event) {
        running = false;
        worker.interrupt();
    }

    public void submit(long orderId) {
        queue.add(orderId);
    }

    public boolean isRunning() {
        return running && worker != null && worker.isAlive();
    }

    /** Milliseconds since the worker last went around its loop. */
    public long millisSinceHeartbeat() {
        return System.currentTimeMillis() - lastHeartbeat.get();
    }

    /** Only the tests call this: a stalled worker stops writing its heartbeat and stops taking orders. */
    public void stall(boolean value) {
        this.stalled = value;
    }

    private void loop() {
        while (running) {
            try {
                if (stalled) {
                    TimeUnit.MILLISECONDS.sleep(50);
                    continue;
                }
                lastHeartbeat.set(System.currentTimeMillis());
                Long id = queue.poll(100, TimeUnit.MILLISECONDS);
                if (id != null) {
                    process(id);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void process(long id) throws InterruptedException {
        // TODO task 3: measure how long processing takes, in a timer named orders.processing
        TimeUnit.MILLISECONDS.sleep(workMillis);
        store.find(id).ifPresent(order -> store.update(order.withStatus("DONE")));
    }
}
