package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 1: names, enum codes and time formats on the wire. */
@QuarkusTest
class ContractTest {

    @Test
    void responsePropertiesAreSnakeCase() {
        Requests.post(Requests.valid()).statusCode(201)
                .body("keySet()", containsInAnyOrder("id", "guest_name", "check_in", "check_out", "room_type", "nights",
                        "total", "deposit", "payment", "created_at"));
    }

    @Test
    void calendarDaysAreIsoTextNotArraysOrNumbers() {
        Requests.post(Requests.valid()).statusCode(201)
                .body("check_in", equalTo("2026-12-01"))
                .body("check_out", equalTo("2026-12-04"));
    }

    @Test
    void aPointInTimeIsAnIsoInstantInUtc() {
        Requests.post(Requests.valid()).statusCode(201)
                .body("created_at", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z"));
    }

    @Test
    void whatWasStoredCanBeReadBackWithTheSameShape() {
        int id = Requests.post(Requests.valid()).extract().path("id");
        given().when().get("/reservations/" + id).then().statusCode(200)
                .body("guest_name", equalTo("Ana"))
                .body("nights", equalTo(3))
                .body("check_in", equalTo("2026-12-01"));
    }

    @Test
    void roomTypesUseTheirPublicCodes() {
        Requests.post(Requests.body("\"2026-12-01\"", "\"single\"", null, null, null)).statusCode(201).body("room_type", equalTo("single"));
        Requests.post(Requests.body("\"2026-12-01\"", "\"double\"", null, null, null)).statusCode(201).body("room_type", equalTo("double"));
        Requests.post(Requests.body("\"2026-12-01\"", "\"suite\"", null, null, null)).statusCode(201).body("room_type", equalTo("suite"));
    }

    @Test
    void theJavaConstantNameIsNotTheContract() {
        Requests.post(Requests.valid()).statusCode(201); // a valid request works, so the 400s below are about the room type
        Requests.post(Requests.body("\"2026-12-01\"", "\"SUITE\"", null, null, null)).statusCode(400);
        Requests.post(Requests.body("\"2026-12-01\"", "\"penthouse\"", null, null, null)).statusCode(400);
    }
}
