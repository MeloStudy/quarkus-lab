package lab.payments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

/** The promise that makes a retry safe: the same key is the same payment, however many times it arrives, even all at once. */
@QuarkusTest
@TestProfile(LogProfile.class)
class IdempotencyTest extends PaymentsTestBase {

    private final String ana = Tokens.customer(Api.ANA);

    @Test
    void theSameKeyAndTheSameRequestReturnTheSamePaymentAndChargeOnce() {
        Response first = Api.pay(ana, "i-1", Api.body(Api.CARD));
        Response second = Api.pay(ana, "i-1", Api.body(Api.CARD));
        Response third = Api.pay(ana, "i-1", Api.body(Api.CARD));

        first.then().statusCode(201);
        assertThat(first.header("Idempotent-Replayed"), is(nullValue()));
        for (Response again : List.of(second, third)) {
            again.then().statusCode(201).header("Idempotent-Replayed", is("true"));
            assertThat(again.asString(), is(first.asString()));
        }
        assertThat("the provider was asked once", Provider.calls(), is(1));
        assertThat("and charged once", Provider.charges(), is(1));
        Api.as(ana).get("/api/payments").then().body("$", hasSize(1));
    }

    @Test
    void theSameKeyWithADifferentRequestIs422AndTouchesNothing() {
        Api.pay(ana, "i-2", Api.body(2500, "EUR", Api.CARD)).then().statusCode(201);

        Api.pay(ana, "i-2", Api.body(9900, "EUR", Api.CARD)).then().statusCode(422).body("code", is("IDEMPOTENCY_KEY_REUSED"));
        Api.pay(ana, "i-2", Api.body(2500, "USD", Api.CARD)).then().statusCode(422);
        Api.pay(ana, "i-2", Api.body(2500, "EUR", "tok_anothercard1")).then().statusCode(422);
        assertThat(Provider.calls(), is(1));
        assertThat(Provider.charges(), is(1));
    }

    @Test
    void theSameKeyOfTwoCustomersAreTwoPayments() {
        String ben = Tokens.customer(Api.BEN);

        String anas = Api.pay(ana, "shared-key", Api.body(Api.CARD)).then().statusCode(201).extract().path("id");
        String bens = Api.pay(ben, "shared-key", Api.body(Api.CARD)).then().statusCode(201).extract().path("id");

        assertThat(anas, is(not(bens)));
        assertThat(Provider.charges(), is(2));
    }

    @Test
    void aChangedDescriptionIsAlsoADifferentRequest() {
        Api.pay(ana, "i-4", "{\"amount\":100,\"currency\":\"EUR\",\"cardToken\":\"" + Api.CARD + "\",\"description\":\"one\"}").then().statusCode(201);

        Api.pay(ana, "i-4", "{\"amount\":100,\"currency\":\"EUR\",\"cardToken\":\"" + Api.CARD + "\",\"description\":\"two\"}").then().statusCode(422);
    }

    /**
     * Ten identical requests at the same moment, against a provider that takes 300 ms. Exactly one of them does the work; the
     * others are told either "still in progress" (409) or, if they arrive after it finished, the same answer again. Whatever the
     * interleaving, the card is charged once.
     */
    @RepeatedTest(5)
    void tenSimultaneousDuplicatesChargeOnce() throws Exception {
        Provider.delay(300);
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Response>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<Response> task = () -> {
                ready.countDown();
                go.await();
                return Api.pay(ana, "i-5", Api.body(Api.CARD));
            };
            futures.add(pool.submit(task));
        }
        ready.await();
        go.countDown();

        int created = 0;
        int inProgress = 0;
        int replayed = 0;
        String paymentId = null;
        for (Future<Response> future : futures) {
            Response response = future.get();
            if (response.statusCode() == 409) {
                inProgress++;
                assertThat(response.path("code"), is("REQUEST_IN_PROGRESS"));
                assertThat(response.header("Retry-After"), is("1"));
            } else {
                response.then().statusCode(201);
                paymentId = paymentId == null ? response.path("id") : paymentId;
                assertThat(response.path("id"), is(paymentId));
                if ("true".equals(response.header("Idempotent-Replayed"))) {
                    replayed++;
                } else {
                    created++;
                }
            }
        }
        pool.shutdown();

        assertThat("exactly one request did the work", created, is(1));
        assertThat(created + inProgress + replayed, is(threads));
        assertThat("the provider was asked once", Provider.calls(), is(1));
        assertThat("and charged once", Provider.charges(), is(1));
    }

    @Test
    void aDeclinedPaymentIsReplayedAsTheSameDecline() {
        Response first = Api.pay(ana, "i-6", Api.body("tok_declined1"));
        Response second = Api.pay(ana, "i-6", Api.body("tok_declined1"));

        first.then().statusCode(402);
        second.then().statusCode(402).header("Idempotent-Replayed", is("true"));
        assertThat(second.path("paymentId"), is((Object) first.path("paymentId")));
        assertThat("the provider heard about it once", Provider.calls(), is(1));
    }
}
