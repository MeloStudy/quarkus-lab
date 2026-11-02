package lab.melostudy;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 2: what the API accepts. Input is strict: anything outside the contract is a 400, never a guess. */
@QuarkusTest
class StrictInputTest {

    /** A starter test that passes for the wrong reason is a bug: first prove that a valid request works at all. */
    @BeforeEach
    void aValidRequestIsAccepted() {
        Requests.post(Requests.valid()).statusCode(201);
    }

    @Test
    void anUnknownPropertyIsRejected() {
        Requests.post(Requests.body("\"2026-12-01\"", "\"suite\"", null, null, "\"vip\":true")).statusCode(400);
    }

    @Test
    void aMisspelledOptionalPropertyDoesNotSilentlyDisappear() {
        // "depsoit" instead of "deposit": the deposit is optional, so without strictness the request would be accepted
        // and the client would believe the deposit was recorded
        Requests.post(Requests.body("\"2026-12-01\"", "\"suite\"", null, null, "\"depsoit\":\"30.00 EUR\"")).statusCode(400);
    }

    @Test
    void anImpossibleDateIsRejected() {
        Requests.post(Requests.body("\"2026-13-45\"", "\"suite\"", null, null, null)).statusCode(400);
    }

    @Test
    void aCalendarDayWithATimeOfDayIsRejected() {
        Requests.post(Requests.body("\"2026-12-01T10:00:00\"", "\"suite\"", null, null, null)).statusCode(400);
    }

    @Test
    void malformedJsonIsAClientError() {
        Requests.post("{\"guest_name\":").statusCode(400);
    }
}
