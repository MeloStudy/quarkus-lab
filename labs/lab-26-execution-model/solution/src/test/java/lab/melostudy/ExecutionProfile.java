package lab.melostudy;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Makes the machine small and the same everywhere: one event loop, four worker threads. With 32 cores the defaults would hide
 * every problem of this lab. The blocked-thread checker is told to complain after 200 ms instead of 2 seconds, so that its
 * warning shows up in the console of a failing test.
 */
public class ExecutionProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.vertx.event-loops-pool-size", "1",
                "quarkus.http.io-threads", "1",
                "quarkus.thread-pool.max-threads", "4",
                "quarkus.vertx.max-event-loop-execute-time", "200ms",
                "quarkus.vertx.warning-exception-time", "200ms");
    }
}
