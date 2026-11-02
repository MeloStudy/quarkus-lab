package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

/** Task 2: optimistic concurrency over HTTP. A write must say which version it is changing (If-Match). */
@QuarkusTest
class ConcurrencyTest {

    @Inject
    TestSupport support;

    @BeforeEach
    void reset() {
        support.reset();
    }

    private static Response put(String url, String ifMatch, String content) {
        var request = given().contentType(ContentType.JSON).body("{\"title\":\"Plan\",\"content\":\"" + content + "\"}");
        if (ifMatch != null) {
            request = request.header("If-Match", ifMatch);
        }
        return request.put(url);
    }

    @Test
    void aWriteWithTheCurrentEtagSucceedsAndReturnsTheNewEtag() {
        Response created = TestSupport.createDocument("Plan", "v1");
        String url = TestSupport.urlOf(created);

        Response updated = put(url, created.getHeader("ETag"), "v2");

        updated.then().statusCode(200).body("content", equalTo("v2"));
        org.junit.jupiter.api.Assertions.assertNotEquals(created.getHeader("ETag"), updated.getHeader("ETag"));
        given().get(url).then().header("ETag", equalTo(updated.getHeader("ETag")));
    }

    @Test
    void aWriteWithoutIfMatchIsRefusedWith428() {
        Response created = TestSupport.createDocument("Plan", "v1");
        String url = TestSupport.urlOf(created);

        put(url, null, "v2").then().statusCode(428);
        given().get(url).then().body("content", equalTo("v1")); // nothing changed
    }

    @Test
    void aWriteBasedOnAnOldVersionIsRefusedWith412AndChangesNothing() {
        Response created = TestSupport.createDocument("Plan", "v1");
        String url = TestSupport.urlOf(created);
        String firstEtag = created.getHeader("ETag");

        put(url, firstEtag, "from Ana").then().statusCode(200); // Ana saves first
        Response late = put(url, firstEtag, "from Ben"); // Ben still has the version he read

        late.then().statusCode(412);
        given().get(url).then().body("content", equalTo("from Ana")); // Ana's work was not overwritten
    }

    @Test
    void theRefusalTellsTheClientWhichVersionIsCurrent() {
        Response created = TestSupport.createDocument("Plan", "v1");
        String url = TestSupport.urlOf(created);
        put(url, created.getHeader("ETag"), "v2").then().statusCode(200);
        String current = given().get(url).getHeader("ETag");

        put(url, created.getHeader("ETag"), "v3").then().statusCode(412).header("ETag", equalTo(current));
    }

    @Test
    void anUnknownDocumentIs404NotAPreconditionError() {
        put("/api/v1/documents/999999", "\"1\"", "x").then().statusCode(404);
    }

    @RepeatedTest(3)
    void ofManySimultaneousWritesWithTheSameEtagExactlyOneWins() throws InterruptedException {
        Response created = TestSupport.createDocument("Plan", "v1");
        String url = TestSupport.urlOf(created);
        String etag = created.getHeader("ETag");

        int writers = 8;
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < writers; i++) {
            int n = i;
            Thread thread = new Thread(() -> {
                try {
                    go.await();
                    int status = put(url, etag, "writer " + n).statusCode();
                    (status == 200 ? ok : status == 412 ? refused : other).incrementAndGet();
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

        org.junit.jupiter.api.Assertions.assertEquals(1, ok.get(), "exactly one writer may win");
        org.junit.jupiter.api.Assertions.assertEquals(writers - 1, refused.get(), "the others get 412 (not 500: " + other.get() + " did)");
    }
}
