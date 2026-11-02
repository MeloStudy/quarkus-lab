package lab.melostudy;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 4: a value type with its own text format, "120.50 EUR". */
@QuarkusTest
class MoneyTest {

    /** A starter test that passes for the wrong reason is a bug: first prove that a valid request works at all. */
    @BeforeEach
    void aValidRequestIsAccepted() {
        Requests.post(Requests.valid()).statusCode(201);
    }

    private static String withDeposit(String deposit) {
        return Requests.body("\"2026-12-01\"", "\"suite\"", deposit, null, null);
    }

    @Test
    void moneyIsWrittenAsOneString() {
        // 3 nights of a suite: 3 x 250.00
        Requests.post(Requests.valid()).statusCode(201).body("total", equalTo("750.00 EUR"));
    }

    @Test
    void moneyIsReadFromThatSameString() {
        Requests.post(withDeposit("\"30.50 EUR\"")).statusCode(201).body("deposit", equalTo("30.50 EUR"));
    }

    @Test
    void aShortAmountIsNormalizedToTwoDecimals() {
        Requests.post(withDeposit("\"100 EUR\"")).statusCode(201).body("deposit", equalTo("100.00 EUR"));
        Requests.post(withDeposit("\"7.5 EUR\"")).statusCode(201).body("deposit", equalTo("7.50 EUR"));
    }

    @Test
    void theDepositIsOptional() {
        Requests.post(Requests.valid()).statusCode(201).body("deposit", equalTo(null));
    }

    @Test
    void malformedMoneyIsAClientErrorNotAServerError() {
        Requests.post(withDeposit("\"abc\"")).statusCode(400);
        Requests.post(withDeposit("\"1.234 EUR\"")).statusCode(400); // a third decimal would be silently rounded
        Requests.post(withDeposit("\"10 eur\"")).statusCode(400);
        Requests.post(withDeposit("100")).statusCode(400); // a bare number has no currency
        Requests.post(withDeposit("{\"amount\":1,\"currency\":\"EUR\"}")).statusCode(400); // the old object form is gone
    }
}
