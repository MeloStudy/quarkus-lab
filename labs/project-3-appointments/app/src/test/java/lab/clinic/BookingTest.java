package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

/**
 * Booking with the fixture of {@link Clinic}: "now" is Monday 2026-03-02 09:00 in Madrid; Dr. Ana works 09:00-13:00 and
 * 15:00-18:00 on weekdays. Every refusal test first books something valid, so none passes for the wrong reason.
 */
@QuarkusTest
@TestSecurity(user = "front-desk", roles = "reception")
class BookingTest {

    private static final String MONDAY_10 = "2026-03-09T09:00:00Z"; // 10:00 in Madrid

    @Inject
    TestData data;

    long ana;
    long ben;
    long checkup; // 30 minutes
    long therapy; // 60 minutes
    long bea;
    long carl;

    @BeforeEach
    void fixture() {
        data.reset();
        Clinic.clockAt(Clinic.NOW);
        ana = Clinic.practitioner("Dr. Ana");
        ben = Clinic.practitioner("Dr. Ben");
        Clinic.weekdayHours(ana);
        Clinic.weekdayHours(ben);
        checkup = Clinic.treatment("Checkup", 30);
        therapy = Clinic.treatment("Therapy", 60);
        bea = Clinic.patient("Bea", "bea@example.com");
        carl = Clinic.patient("Carl", "carl@example.com");
        // the baseline of every refusal below
        Clinic.book(ana, bea, checkup, "2026-03-10T09:00:00Z").then().statusCode(201);
    }

    @Test
    void aValidBookingAnswers201WithLocationEtagAndTheTimeInTheClinicsZone() {
        Clinic.book(ana, bea, checkup, MONDAY_10).then().statusCode(201).header("Location", endsWith("/api/appointments/2"))
                .header("ETag", equalTo("\"0\"")).body("status", equalTo("BOOKED"))
                .body("startsAt", equalTo(MONDAY_10)).body("endsAt", equalTo("2026-03-09T09:30:00Z"))
                .body("local", equalTo("2026-03-09T10:00:00+01:00")).body("practitionerName", equalTo("Dr. Ana"))
                .body("treatmentName", equalTo("Checkup")).body("version", equalTo(0));
    }

    @Test
    void anOffsetIsAcceptedAndMeansTheSameInstant() {
        Clinic.book(ana, carl, checkup, "2026-03-09T10:00:00+01:00").then().statusCode(201).body("startsAt", equalTo(MONDAY_10));
    }

    @Test
    void theSamePractitionerCannotBeBookedTwiceAtTheSameTime() {
        Clinic.book(ana, bea, checkup, MONDAY_10).then().statusCode(201);

        Clinic.book(ana, carl, checkup, MONDAY_10).then().statusCode(409).body("code", equalTo("SLOT_TAKEN"));
    }

    @Test
    void anOverlapOfOneMinuteIsStillAnOverlapButBackToBackIsFine() {
        Clinic.book(ana, bea, therapy, MONDAY_10).then().statusCode(201); // 10:00-11:00

        Clinic.book(ana, carl, checkup, "2026-03-09T09:45:00Z").then().statusCode(409).body("code", equalTo("SLOT_TAKEN"));
        Clinic.book(ana, carl, checkup, "2026-03-09T10:00:00Z").then().statusCode(201); // 11:00-11:30
    }

    @Test
    void aLongTreatmentConflictsWithAShortOneInsideIt() {
        Clinic.book(ana, bea, checkup, "2026-03-09T09:15:00Z").then().statusCode(201); // 10:15-10:45

        Clinic.book(ana, carl, therapy, MONDAY_10).then().statusCode(409).body("code", equalTo("SLOT_TAKEN")); // 10:00-11:00
    }

    @Test
    void aPatientCannotBeBookedWithTwoPractitionersAtOnce() {
        Clinic.book(ana, bea, checkup, MONDAY_10).then().statusCode(201);

        Clinic.book(ben, bea, checkup, MONDAY_10).then().statusCode(409).body("code", equalTo("PATIENT_BUSY"));
        Clinic.book(ben, carl, checkup, MONDAY_10).then().statusCode(201); // the other practitioner is free for somebody else
    }

    @Test
    void theStartMustBeOnTheQuarterHourOfTheClinicsClock() {
        Clinic.book(ana, carl, checkup, "2026-03-09T09:10:00Z").then().statusCode(422).body("code", equalTo("INVALID_START"));
        Clinic.book(ana, carl, checkup, "2026-03-09T09:00:30Z").then().statusCode(422).body("code", equalTo("INVALID_START"));
    }

    @Test
    void thePastAndTheTooFarFutureAreRefused() {
        Clinic.book(ana, carl, checkup, "2026-03-02T07:45:00Z").then().statusCode(422).body("code", equalTo("IN_THE_PAST"));
        Clinic.book(ana, carl, checkup, "2026-06-08T09:00:00Z").then().statusCode(422).body("code", equalTo("TOO_FAR"));
    }

    @Test
    void theWorkingHoursOfThatPractitionerOnThatDayDecide() {
        Clinic.book(ana, carl, checkup, "2026-03-09T07:30:00Z").then().statusCode(422)
                .body("code", equalTo("OUTSIDE_WORKING_HOURS")); // 08:30, before opening
        Clinic.book(ana, carl, therapy, "2026-03-09T11:30:00Z").then().statusCode(422)
                .body("code", equalTo("OUTSIDE_WORKING_HOURS")); // 12:30-13:30 crosses the end of the morning
        Clinic.book(ana, carl, checkup, "2026-03-09T12:00:00Z").then().statusCode(422)
                .body("code", equalTo("OUTSIDE_WORKING_HOURS")); // 13:00, lunch
        Clinic.book(ana, carl, checkup, "2026-03-14T09:00:00Z").then().statusCode(422)
                .body("code", equalTo("OUTSIDE_WORKING_HOURS")); // a Saturday
        Clinic.book(ana, carl, therapy, "2026-03-09T11:00:00Z").then().statusCode(201); // 12:00-13:00 ends exactly at closing
    }

    @Test
    void theWorkingHoursFollowTheClocksNotTheUtcOffsetOfJanuary() {
        // 09:00 local on Monday 2026-03-30 is 07:00Z (summer time, +02:00): the morning opens one UTC hour earlier
        Clinic.book(ana, carl, checkup, "2026-03-30T07:00:00Z").then().statusCode(201).body("local", equalTo("2026-03-30T09:00:00+02:00"));
        Clinic.book(ana, bea, checkup, "2026-03-30T06:30:00Z").then().statusCode(422).body("code", equalTo("OUTSIDE_WORKING_HOURS"));
    }

    @Test
    void unknownPractitionerPatientOrTreatmentIsA404BeforeAnyRuleAboutTime() {
        // the time is also invalid (the past): existence is checked first
        Clinic.book(999, carl, checkup, "2026-03-01T09:00:00Z").then().statusCode(404).body("code", equalTo("NOT_FOUND"));
        Clinic.book(ana, 999, checkup, "2026-03-01T09:00:00Z").then().statusCode(404);
        Clinic.book(ana, carl, 999, "2026-03-01T09:00:00Z").then().statusCode(404);
    }

    @Test
    void theOrderOfTheTimeRulesIsFixedGridThenPastThenHorizonThenHours() {
        // a start that breaks everything at once reports the first rule in the documented order
        Clinic.book(ana, carl, checkup, "2025-01-01T03:07:00Z").then().statusCode(422).body("code", equalTo("INVALID_START"));
        Clinic.book(ana, carl, checkup, "2025-01-01T03:00:00Z").then().statusCode(422).body("code", equalTo("IN_THE_PAST"));
    }

    @Test
    void aRefusedBookingLeavesNothingBehind() {
        long before = data.count("appointment");

        Clinic.book(ana, carl, checkup, "2026-03-09T07:30:00Z").then().statusCode(422);
        Clinic.book(ana, bea, checkup, "2026-03-10T09:00:00Z").then().statusCode(409);

        org.junit.jupiter.api.Assertions.assertEquals(before, data.count("appointment"));
        given().get("/api/appointments/1").then().statusCode(200).body("id", notNullValue());
    }
}
