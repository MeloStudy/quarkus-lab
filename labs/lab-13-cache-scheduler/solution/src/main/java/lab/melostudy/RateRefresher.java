package lab.melostudy;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Periodic job: "pulls" fresh rates from upstream (simulated by sleeping) and then empties the cache.
 * The counters exist so the tests can see how the scheduler behaves; they are not part of the topic.
 */
@ApplicationScoped
public class RateRefresher {

    private final RateService rates;
    private final Duration duration;
    private final AtomicInteger executions = new AtomicInteger();
    private final AtomicInteger running = new AtomicInteger();
    private final AtomicInteger maxConcurrent = new AtomicInteger();

    RateRefresher(RateService rates, @ConfigProperty(name = "rates.refresh.duration") Duration duration) {
        this.rates = rates;
        this.duration = duration;
    }

    /**
     * The interval comes from configuration ({@code off} disables the job, which is what the tests do).
     * SKIP: if the previous run is still going, this fire time is dropped instead of running in parallel.
     */
    @Scheduled(every = "{rates.refresh.every}", concurrentExecution = SKIP, identity = "rates-refresh")
    void refresh() throws InterruptedException {
        int now = running.incrementAndGet();
        maxConcurrent.accumulateAndGet(now, Math::max);
        try {
            Thread.sleep(duration.toMillis());
            rates.forgetAll();
            executions.incrementAndGet();
        } finally {
            running.decrementAndGet();
        }
    }

    public int executions() {
        return executions.get();
    }

    public int maxConcurrent() {
        return maxConcurrent.get();
    }
}
