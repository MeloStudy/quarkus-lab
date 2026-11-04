package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;

@QuarkusTest
class AvailabilityTest {

    @Inject
    TestData data;

    long ana;
    long checkup; // 30 minutes
    long bea;

    @BeforeEach
    void fixture() {
        data.reset();
        Clinic.clockAt(Clinic.NOW);
        ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        checkup = Clinic.treatment("Checkup", 30);
        bea = Clinic.patient("Bea", "bea@example.com");
    }

    private Response day(String date) {
        return given().queryParam("date", date).queryParam("treatmentId", checkup)
                .get("/api/practitioners/" + ana + "/availability");
    }

    @Test
    void aWorkingDayListsEverySlotInTheClinicsTimeWithItsOffset() {
        day("2026-03-09").then().statusCode(200).body("timeZone", equalTo("Europe/Madrid")).body("durationMinutes", equalTo(30))
                .body("slots", hasSize(15 + 11)) // 09:00-12:30 and 15:00-17:30
                .body("slots[0].startsAt", equalTo("2026-03-09T08:00:00Z"))
                .body("slots[0].local", equalTo("2026-03-09T09:00:00+01:00"))
                .body("slots[15].local", equalTo("2026-03-09T15:00:00+01:00"));
    }

    @Test
    void theLunchBreakAndTheEveningHaveNoSlots() {
        day("2026-03-09").then().body("slots.local", not(hasItem("2026-03-09T12:45:00+01:00")))
                .body("slots.local", not(hasItem("2026-03-09T14:00:00+01:00")))
                .body("slots.local", not(hasItem("2026-03-09T17:45:00+01:00")))
                .body("slots.local", hasItem("2026-03-09T17:30:00+01:00"));
    }

    @Test
    void aBookingRemovesTheSlotsItTouchesAndNoMore() {
        Clinic.booked(ana, bea, checkup, "2026-03-09T09:00:00Z"); // 10:00-10:30

        day("2026-03-09").then().body("slots", hasSize(26 - 3)).body("slots.local", hasItem("2026-03-09T09:30:00+01:00"))
                .body("slots.local", not(hasItem("2026-03-09T09:45:00+01:00")))
                .body("slots.local", not(hasItem("2026-03-09T10:00:00+01:00")))
                .body("slots.local", not(hasItem("2026-03-09T10:15:00+01:00")))
                .body("slots.local", hasItem("2026-03-09T10:30:00+01:00"));
    }

    @Test
    void aCancelledBookingGivesItsSlotsBack() {
        long id = Clinic.booked(ana, bea, checkup, "2026-03-09T09:00:00Z");
        Clinic.act(id, Clinic.etagOf(id), "status", java.util.Map.of("to", "CANCELLED")).then().statusCode(200);

        day("2026-03-09").then().body("slots", hasSize(26));
    }

    @Test
    void slotsThatAlreadyPassedAreNotOffered() {
        Clinic.clockAt("2026-03-02T10:10:00Z"); // 11:10 in Madrid

        day("2026-03-02").then().body("slots[0].local", equalTo("2026-03-02T11:15:00+01:00"));
    }

    @Test
    void aDayWithoutWorkingHoursAndADayBeyondTheHorizonAreEmptyNotErrors() {
        day("2026-03-08").then().statusCode(200).body("slots", empty()); // a Sunday
        day("2026-06-30").then().statusCode(200).body("slots", empty()); // 120 days ahead, the horizon is 90
    }

    @Test
    void theDayTheClocksGoForwardHasFiveHoursOfNightNotSix() {
        long night = Clinic.practitioner("Dr. Night");
        Clinic.hours(night, Clinic.block(7, "00:00", "06:00")); // Sundays
        long quarter = Clinic.treatment("Quick", 15);

        given().queryParam("date", "2026-03-29").queryParam("treatmentId", quarter)
                .get("/api/practitioners/" + night + "/availability").then().statusCode(200).body("slots", hasSize(20))
                .body("slots[0].local", equalTo("2026-03-29T00:00:00+01:00"))
                .body("slots[7].local", equalTo("2026-03-29T01:45:00+01:00"))
                .body("slots[8].local", equalTo("2026-03-29T03:00:00+02:00")) // 02:00 does not exist
                .body("slots[19].local", equalTo("2026-03-29T05:45:00+02:00"));
    }

    @Test
    void theDayTheClocksGoBackHasTwoTwoOClocksAndTheOffsetTellsThemApart() {
        Clinic.clockAt("2026-10-19T08:00:00Z");
        long night = Clinic.practitioner("Dr. Night");
        Clinic.hours(night, Clinic.block(7, "00:00", "06:00"));
        long quarter = Clinic.treatment("Quick", 15);

        given().queryParam("date", "2026-10-25").queryParam("treatmentId", quarter)
                .get("/api/practitioners/" + night + "/availability").then().statusCode(200).body("slots", hasSize(28))
                .body("slots.local", hasItem("2026-10-25T02:00:00+02:00"))
                .body("slots.local", hasItem("2026-10-25T02:00:00+01:00"))
                .body("slots.startsAt", hasItem("2026-10-25T00:00:00Z"))
                .body("slots.startsAt", hasItem("2026-10-25T01:00:00Z"));
    }

    @Test
    void badParametersAreClientErrorsThatNameTheMistake() {
        day("2026-02-30").then().statusCode(400).body("code", equalTo("INVALID_DATE"));
        day("tomorrow").then().statusCode(400).body("code", equalTo("VALIDATION_ERROR")).body("errors.field", contains("date"));
        given().queryParam("date", "2026-03-09").get("/api/practitioners/" + ana + "/availability").then().statusCode(400)
                .body("errors.field", contains("treatmentId"));
        given().queryParam("date", "2026-03-09").queryParam("treatmentId", 999)
                .get("/api/practitioners/" + ana + "/availability").then().statusCode(404);
        given().queryParam("date", "2026-03-09").queryParam("treatmentId", checkup)
                .get("/api/practitioners/999/availability").then().statusCode(404);
    }

    @Test
    void everySlotOfTheResponseCanReallyBeBooked() {
        List<String> starts = day("2026-03-10").then().extract().jsonPath().getList("slots.startsAt");

        // the contract between this endpoint and the booking rules: what is offered is accepted
        Clinic.book(ana, bea, checkup, starts.get(0)).then().statusCode(201);
        Clinic.book(ana, bea, checkup, starts.get(starts.size() - 1)).then().statusCode(201);
    }
}
