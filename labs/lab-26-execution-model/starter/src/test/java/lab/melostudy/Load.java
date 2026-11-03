package lab.melostudy;

import static io.restassured.RestAssured.given;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

import io.restassured.response.Response;

/** Sends many requests at the same moment and measures how long it takes until the last one is answered. */
final class Load {

    record Result(long millis, List<Response> responses) {
    }

    private Load() {
    }

    static Result run(int requests, IntFunction<String> path) {
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Response>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                String target = path.apply(i);
                futures.add(pool.submit(() -> {
                    go.await();
                    return given().get(target);
                }));
            }
            long start = System.nanoTime();
            go.countDown();
            List<Response> responses = new ArrayList<>();
            for (Future<Response> future : futures) {
                responses.add(future.get());
            }
            return new Result((System.nanoTime() - start) / 1_000_000, responses);
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        } finally {
            pool.shutdownNow();
        }
    }

    /** Starts the requests and returns at once; call {@link Pending#finish()} to collect them. */
    static Pending start(int requests, IntFunction<String> path) {
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        List<Future<Response>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            String target = path.apply(i);
            futures.add(pool.submit(() -> given().get(target)));
        }
        return new Pending(pool, futures);
    }

    record Pending(ExecutorService pool, List<Future<Response>> futures) {

        List<Response> finish() {
            try {
                List<Response> responses = new ArrayList<>();
                for (Future<Response> future : futures) {
                    responses.add(future.get());
                }
                return responses;
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            } finally {
                pool.shutdownNow();
            }
        }
    }

    /** How long a single request takes to be answered, in milliseconds. */
    static long latency(String path) {
        long start = System.nanoTime();
        given().get(path).then().statusCode(200);
        return (System.nanoTime() - start) / 1_000_000;
    }
}
