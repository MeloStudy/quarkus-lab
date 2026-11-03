package lab.payments;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/** What the platform asks (lab 24), answered for a service whose one dependency is allowed to be down. */
@QuarkusTest
@TestProfile(LogProfile.class)
class HealthTest extends PaymentsTestBase {

    @Test
    void operationsEndpointsLiveOnTheManagementPortOnly() {
        Ops.get("/q/health/ready").then().statusCode(200);
        Ops.onPublicPort("/q/health").then().statusCode(404);
        Ops.onPublicPort("/q/metrics").then().statusCode(404);
    }

    @Test
    void livenessLooksAtNothingButTheProcess() {
        Ops.get("/q/health/live").then().statusCode(200).body("status", is("UP")).body("checks", is(empty()));

        // Even with the provider gone and the breaker open: restarting this process would fix nothing.
        Provider.down();
        openTheBreaker();
        Ops.get("/q/health/live").then().statusCode(200).body("status", is("UP"));
    }

    @Test
    void readinessShowsTheStateOfTheProviderBreaker() {
        Ops.get("/q/health/ready").then().statusCode(200).body("status", is("UP"))
                .body("checks[0].name", is("provider")).body("checks[0].data.circuit", is("CLOSED"));
    }

    @Test
    void readinessStaysUpWhileTheBreakerIsOpenBecauseReadingPaymentsDoesNotNeedTheProvider() {
        Api.pay("h-1", Api.CARD).then().statusCode(201);
        openTheBreaker();

        Ops.get("/q/health/ready").then().statusCode(200).body("status", is("UP"))
                .body("checks[0].data.circuit", is("OPEN"));
        Api.as(Tokens.customer(Api.ANA)).get("/api/payments").then().statusCode(200);
    }
}
