package lab.clinic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

/** Filters, paging and the number of SQL statements of a page. */
@QuarkusTest
@TestSecurity(user = "front-desk", roles = "reception")
class ListingTest {

    @Inject
    TestData data;

    @Inject
    QueryCounter queries;

    long ana;
    long ben;
    long checkup;
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
        bea = Clinic.patient("Bea", "bea@example.com");
        carl = Clinic.patient("Carl", "carl@example.com");
    }

    /** Ten appointments on Monday 2026-03-09, one every 30 minutes from 09:00 (local), alternating the practitioners. */
    private void tenAppointments() {
        for (int i = 0; i < 10; i++) {
            String start = "2026-03-09T0" + (8 + i / 2) + ":" + (i % 2 == 0 ? "00" : "30") + ":00Z";
            if (i >= 8) {
                start = "2026-03-09T12:" + (i % 2 == 0 ? "00" : "30") + ":00Z"; // 13:00 is lunch: keep to the afternoon
            }
            Clinic.booked(i % 2 == 0 ? ana : ben, i % 4 < 2 ? bea : carl, checkup, start.length() == 20 ? start : start);
        }
    }

    @Test
    void theListIsOrderedByStartAndPagedInTheDatabase() {
        Clinic.booked(ana, bea, checkup, "2026-03-10T09:00:00Z");
        Clinic.booked(ana, carl, checkup, "2026-03-09T09:00:00Z");
        Clinic.booked(ana, bea, checkup, "2026-03-11T09:00:00Z");

        given().queryParam("size", 2).get("/api/appointments").then().statusCode(200).body("total", equalTo(3))
                .body("page", equalTo(0)).body("size", equalTo(2)).body("items", hasSize(2))
                .body("items.startsAt", contains("2026-03-09T09:00:00Z", "2026-03-10T09:00:00Z"));
        given().queryParams(Map.of("size", 2, "page", 1)).get("/api/appointments").then().body("items", hasSize(1))
                .body("items[0].startsAt", equalTo("2026-03-11T09:00:00Z"));
    }

    @Test
    void filtersCombine() {
        Clinic.booked(ana, bea, checkup, "2026-03-09T09:00:00Z");
        Clinic.booked(ben, bea, checkup, "2026-03-10T09:00:00Z");
        long cancelled = Clinic.booked(ana, carl, checkup, "2026-03-11T09:00:00Z");
        Clinic.act(cancelled, Clinic.etagOf(cancelled), "status", Map.of("to", "CANCELLED")).then().statusCode(200);

        given().queryParam("practitionerId", ana).get("/api/appointments").then().body("total", equalTo(2));
        given().queryParam("patientId", bea).get("/api/appointments").then().body("total", equalTo(2));
        given().queryParam("status", "CANCELLED").get("/api/appointments").then().body("total", equalTo(1))
                .body("items[0].id", equalTo((int) cancelled));
        given().queryParams(Map.of("practitionerId", ana, "patientId", bea)).get("/api/appointments").then()
                .body("total", equalTo(1));
    }

    @Test
    void aDateRangeIsMadeOfDaysOfTheClinicAndBothEndsAreIncluded() {
        Clinic.booked(ana, bea, checkup, "2026-03-09T08:00:00Z"); // Mon 09:00
        Clinic.booked(ana, bea, checkup, "2026-03-10T16:30:00Z"); // Tue 17:30, still Tuesday in Madrid
        Clinic.booked(ana, bea, checkup, "2026-03-11T08:00:00Z"); // Wed 09:00

        given().queryParams(Map.of("from", "2026-03-10", "to", "2026-03-10")).get("/api/appointments").then()
                .body("total", equalTo(1)).body("items[0].local", equalTo("2026-03-10T17:30:00+01:00"));
        given().queryParams(Map.of("from", "2026-03-09", "to", "2026-03-10")).get("/api/appointments").then().body("total", equalTo(2));
    }

    @Test
    void badFiltersAreClientErrorsThatNameTheParameter() {
        given().queryParam("status", "DONE").get("/api/appointments").then().statusCode(400).body("errors.field", contains("status"));
        given().queryParam("page", "abc").get("/api/appointments").then().statusCode(400).body("errors.field", contains("page"));
        given().queryParam("size", 500).get("/api/appointments").then().statusCode(400).body("errors.field", contains("size"));
        given().queryParam("from", "2026-02-30").get("/api/appointments").then().statusCode(400).body("code", equalTo("INVALID_DATE"));
    }

    @Test
    void aPageOfTwentyRowsCostsTwoStatementsNotTwentyThree() {
        for (int i = 0; i < 20; i++) {
            String start = String.format("2026-03-%02dT09:%02d:00Z", 9 + i / 4 + (i / 4 >= 5 ? 2 : 0), (i % 4) * 15);
            Clinic.booked(i % 2 == 0 ? ana : ben, i % 2 == 0 ? bea : carl, checkup, start);
        }
        given().queryParam("size", 20).get("/api/appointments").then().statusCode(200).body("items", hasSize(20));

        queries.reset();
        given().queryParam("size", 20).get("/api/appointments").then().statusCode(200).body("items", hasSize(20))
                .body("items[0].practitionerName", equalTo("Dr. Ana"));

        // one COUNT and one SELECT with the three associations in a join; with lazy loading it would be 1 + 20 x 3
        assertTrue(queries.statements() <= 2, "statements: " + queries.statements());
    }
}
