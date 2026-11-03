package lab.tracker;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Races between real requests. Each one asserts exact numbers, not "no exception". */
@QuarkusTest
class ConcurrencyTest {

    @Inject
    TestData data;

    @BeforeEach
    void projects() {
        data.reset();
        Tracker.project("TRK").then().statusCode(201);
    }

    /** Runs the tasks at the same instant and returns their results. */
    private static <T> List<T> race(int n, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            CountDownLatch ready = new CountDownLatch(n);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }
            ready.await();
            go.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void twentySimultaneousIssuesGetTwentyConsecutiveDistinctNumbers() throws Exception {
        List<String> keys = race(20, () -> {
            var r = Tracker.open("TRK", "{\"title\":\"parallel\",\"type\":\"TASK\"}");
            assertEquals(201, r.statusCode());
            return r.jsonPath().getString("key");
        });

        Set<String> expected = new TreeSet<>();
        for (int i = 1; i <= 20; i++) {
            expected.add("TRK-" + i);
        }
        assertEquals(expected, new TreeSet<>(keys));
        assertEquals(20, given().get("/api/projects/TRK/summary").jsonPath().getLong("total"));
    }

    @Test
    void sixWritersWithTheSameEtagAndExactlyOneWins() throws Exception {
        String key = Tracker.issue("TRK", "contended");
        String etag = Tracker.etag(key);
        int[] counter = { 0 };

        List<Integer> statuses = race(6, () -> {
            int n;
            synchronized (counter) {
                n = ++counter[0];
            }
            return Tracker.put(key, etag, Tracker.updateBody("writer " + n, "HIGH", null)).statusCode();
        });

        assertEquals(1, statuses.stream().filter(s -> s == 200).count(), "statuses: " + statuses);
        assertEquals(5, statuses.stream().filter(s -> s == 412).count(), "statuses: " + statuses);
        assertEquals(1, Tracker.get(key).jsonPath().getLong("version"), "only the winner moved the version");
    }

    @Test
    void twoPeopleStartingTheSameIssueAtOnceStartItOnce() throws Exception {
        String key = Tracker.issue("TRK", "start me");
        Tracker.assign(key, "alice");
        String etag = Tracker.etag(key);

        List<Integer> statuses = race(2, () -> Tracker.transition(key, etag, "IN_PROGRESS").statusCode());

        assertEquals(1, statuses.stream().filter(s -> s == 200).count(), "statuses: " + statuses);
        assertEquals(1, statuses.stream().filter(s -> s == 412).count(), "statuses: " + statuses);
        assertEquals(1, given().get("/api/issues/{key}/history", key).jsonPath()
                .getList("findAll { it.kind == 'STATUS_CHANGED' }").size(), "one status event, not two");
    }
}
