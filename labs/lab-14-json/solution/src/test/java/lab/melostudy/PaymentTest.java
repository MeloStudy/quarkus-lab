package lab.melostudy;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 3: a payload whose shape depends on a discriminator. */
@QuarkusTest
class PaymentTest {

    /** A starter test that passes for the wrong reason is a bug: first prove that a valid request works at all. */
    @BeforeEach
    void aValidRequestIsAccepted() {
        Requests.post(Requests.valid()).statusCode(201);
    }

    private static String withPayment(String payment) {
        return Requests.body("\"2026-12-01\"", "\"suite\"", null, payment, null);
    }

    @Test
    void aCardPaymentRoundTrips() {
        Requests.post(withPayment("{\"method\":\"card\",\"last4\":\"4242\"}")).statusCode(201)
                .body("payment.method", equalTo("card"))
                .body("payment.last4", equalTo("4242"));
    }

    @Test
    void aTransferPaymentRoundTrips() {
        Requests.post(withPayment("{\"method\":\"transfer\",\"iban\":\"ES9121000418450200051332\"}")).statusCode(201)
                .body("payment.method", equalTo("transfer"))
                .body("payment.iban", equalTo("ES9121000418450200051332"));
    }

    @Test
    void thePaymentIsOptional() {
        Requests.post(Requests.valid()).statusCode(201).body("payment", equalTo(null));
    }

    @Test
    void anUnknownMethodIsRejected() {
        Requests.post(withPayment("{\"method\":\"bitcoin\"}")).statusCode(400);
    }

    @Test
    void aMissingMethodIsRejected() {
        Requests.post(withPayment("{\"last4\":\"4242\"}")).statusCode(400);
    }

    @Test
    void aJavaClassNameIsNotAValidMethod() {
        // The discriminator picks among the types WE declared. It must never be a class the client names.
        Requests.post(withPayment("{\"method\":\"java.lang.ProcessBuilder\"}")).statusCode(400);
        Requests.post(withPayment("{\"method\":\"lab.melostudy.Payment$Card\",\"last4\":\"1\"}")).statusCode(400);
    }
}
