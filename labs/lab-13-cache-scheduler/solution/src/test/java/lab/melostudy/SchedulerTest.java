package lab.melostudy;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

/**
 * Task 3: a job that takes longer than its interval. The profile makes the job fire every second and work for
 * 1.5 seconds; the other tests keep it off. (Test profiles are the subject of lab 17; here it is only a switch.)
 */
@QuarkusTest
@TestProfile(SchedulerTest.FastRefresh.class)
class SchedulerTest {

    public static class FastRefresh implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("rates.refresh.every", "1s", "rates.refresh.duration", "1500ms");
        }
    }

    @Inject
    RateRefresher refresher;

    @Test
    void aSlowJobNeverOverlapsItself() {
        await().atMost(Duration.ofSeconds(15)).until(() -> refresher.executions() >= 2);

        assertThat(refresher.maxConcurrent(), equalTo(1)); // never two runs at the same time
    }
}
