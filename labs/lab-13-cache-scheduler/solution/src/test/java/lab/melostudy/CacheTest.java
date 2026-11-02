package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Task 1: reads are served from the cache. The upstream call counter tells where each answer came from. */
@QuarkusTest
class CacheTest {

    @Inject
    RateTestSupport support;

    @Inject
    RateProvider provider;

    @BeforeEach
    void reset() {
        support.reset();
    }

    @Test
    void repeatedReadsOfOneCurrencyCallUpstreamOnce() {
        for (int i = 0; i < 3; i++) {
            given().when().get("/rates/EUR").then().statusCode(200).body("rate", equalTo(1.0f));
        }
        assertThat(provider.calls(), equalTo(1));
    }

    @Test
    void eachCurrencyIsCachedUnderItsOwnKey() {
        given().when().get("/rates/EUR").then().body("rate", equalTo(1.0f));
        given().when().get("/rates/USD").then().body("rate", equalTo(1.1f));
        given().when().get("/rates/EUR").then().body("rate", equalTo(1.0f));
        given().when().get("/rates/USD").then().body("rate", equalTo(1.1f));
        assertThat(provider.calls(), equalTo(2));
    }

    @Test
    void aQuoteReusesTheCachedRate() {
        for (int i = 1; i <= 3; i++) {
            given().queryParam("amount", 100 * i).when().get("/rates/USD/quote")
                    .then().statusCode(200).body("total", equalTo(110.0f * i));
        }
        assertThat(provider.calls(), equalTo(1));
    }

    @Test
    void tenSimultaneousFirstReadsCallUpstreamOnce() throws InterruptedException {
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                try {
                    go.await();
                    given().when().get("/rates/PEN").then().statusCode(200);
                    ok.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(thread);
            thread.start();
        }
        go.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
        assertThat(ok.get(), equalTo(10));
        assertThat(provider.calls(), equalTo(1));
    }

    @Test
    void failuresAreNotCached() {
        given().when().get("/rates/XXX").then().statusCode(404);
        given().when().get("/rates/XXX").then().statusCode(404);
        assertThat(provider.calls(), equalTo(2)); // both attempts went upstream
    }
}
