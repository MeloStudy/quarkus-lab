package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;

/**
 * The whole application through HTTP, with the two collaborators that talk to the outside world replaced by mocks:
 * the fraud service (remote, never reachable in a test) and the notifier (a side effect we want to observe).
 */
@QuarkusTest
class RewardsResourceTest {

    @InjectMock
    FraudClient fraud;

    @InjectMock
    Notifier notifier;

    @Inject
    RewardsMetrics metrics;

    @Inject
    AccountRepository accounts;

    @BeforeEach
    @AfterEach
    void cleanTheSharedState() {
        // The database and RewardsMetrics live as long as the application, not as long as a test.
        QuarkusTransaction.requiringNew().run(() -> accounts.deleteAll());
        metrics.reset();
    }

    private static ValidatableResponse order(String customerId, String amount) {
        return given().contentType(ContentType.JSON)
                .body("{\"customerId\":\"" + customerId + "\",\"amount\":" + amount + "}")
                .when().post("/rewards/orders").then();
    }

    // ---- the happy path, and what we can observe about it

    @Test
    void anOrderThatPassesTheFraudCheckGivesPoints() {
        when(fraud.isSuspicious("ana", new BigDecimal("125"))).thenReturn(false);

        order("ana", "125").statusCode(201).body("points", equalTo(12)).body("balance", equalTo(12));

        verify(fraud, times(1)).isSuspicious("ana", new BigDecimal("125"));
        verify(notifier).send("ana", "You earned 12 points");
    }

    @Test
    void pointsAccumulateAcrossOrders() {
        order("ana", "125").statusCode(201);
        order("ana", "55").statusCode(201).body("points", equalTo(5)).body("balance", equalTo(17));

        given().get("/rewards/ana").then().statusCode(200).body("points", equalTo(17));
    }

    @Test
    void aMockStartsEveryTestWithoutTheStubbingOfThePreviousOne() {
        // fraudIsSuspiciousForOneCustomerOnly stubs this same method to "true"; none of that is visible here
        order("ben", "100").statusCode(201);
    }

    // ---- collaborators that say no, or fail

    @Test
    void fraudIsSuspiciousForOneCustomerOnly() {
        when(fraud.isSuspicious(eq("mallory"), any())).thenReturn(true);

        order("mallory", "500").statusCode(409);
        order("ana", "500").statusCode(201);

        given().get("/rewards/mallory").then().statusCode(404); // nothing was written
        verify(notifier, never()).send(eq("mallory"), anyString());
    }

    @Test
    void whenTheFraudServiceIsDownTheAnswerIs503AndNothingIsWritten() {
        when(fraud.isSuspicious(anyString(), any())).thenThrow(new FraudServiceUnavailableException());

        order("ana", "125").statusCode(503);

        given().get("/rewards/ana").then().statusCode(404);
        verifyNoInteractions(notifier);
    }

    @Test
    void aFailingNotificationDoesNotUndoTheAward() {
        doThrow(new RuntimeException("smtp is down")).when(notifier).send(anyString(), anyString());

        order("ana", "125").statusCode(201).body("balance", equalTo(12));

        given().get("/rewards/ana").then().statusCode(200).body("points", equalTo(12));
    }

    @Test
    void anInvalidOrderNeverReachesTheFraudService() {
        order("ana", "0").statusCode(400);
        order("ana", "-5").statusCode(400);

        verifyNoInteractions(fraud);
    }

    // ---- time

    @Test
    void ordersOnASaturdayGiveDoublePoints() {
        // 2026-11-07 is a Saturday. The mock applies to this test only.
        QuarkusMock.installMockForType(Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC), Clock.class);

        order("ana", "125").statusCode(201).body("points", equalTo(24));
    }

    @Test
    void ordersOnAWednesdayGiveNormalPoints() {
        QuarkusMock.installMockForType(Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneOffset.UTC), Clock.class);

        order("ana", "125").statusCode(201).body("points", equalTo(12));
    }

    // ---- state that lives as long as the application

    @Test
    void theMetricsCountOneAwardAfterOneOrder() {
        order("ana", "125").statusCode(201);

        assertEquals(1, metrics.awarded());
    }

    @Test
    void theMetricsCountTwoAwardsAfterTwoOrders() {
        order("ana", "125").statusCode(201);
        order("ben", "125").statusCode(201);

        assertEquals(2, metrics.awarded());
    }
}
