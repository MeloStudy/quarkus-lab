package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

/** The one test where nobody calls the job: the scheduler does, every second, and the reminder goes out by itself. */
@QuarkusTest
@QuarkusTestResource(NotificationStub.class)
@TestProfile(ReminderSchedulerTest.EverySecond.class)
@TestSecurity(user = "front-desk", roles = "reception")
class ReminderSchedulerTest {

    public static class EverySecond implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("clinic.reminders.poll-interval", "1s");
        }
    }

    @Inject
    TestData data;

    @BeforeEach
    void fixture() {
        data.reset();
        Provider.reset();
        Clinic.clockAt(Clinic.NOW);
    }

    @Test
    void aDueReminderIsSentByTheSchedulerWithoutAnybodyAskingForIt() throws Exception {
        long ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        long checkup = Clinic.treatment("Checkup", 30);
        long bea = Clinic.patient("Bea", "bea@example.com");
        Clinic.booked(ana, bea, checkup, "2026-03-02T10:00:00Z"); // due at once

        String status = "PENDING";
        for (int i = 0; i < 40 && !"SENT".equals(status); i++) {
            Thread.sleep(500);
            status = given().get("/api/reminders").then().extract().path("[0].status");
        }
        if (!"SENT".equals(status)) {
            fail("the scheduler never sent the reminder; status is " + status);
        }
        assertEquals(1, Provider.deliveries());
    }
}
