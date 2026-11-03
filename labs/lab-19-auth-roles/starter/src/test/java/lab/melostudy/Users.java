package lab.melostudy;

import static io.restassured.RestAssured.given;

import io.restassured.specification.RequestSpecification;

/**
 * Test-only: the demo users of the dev/test profile (alice = admin, bob and carol = user, dave = no roles at all;
 * every password is {@code <name>-pw}). {@code preemptive()} sends the Authorization header on the FIRST request;
 * without it RestAssured waits for a 401 challenge and only then sends the credentials.
 */
final class Users {

    private Users() {
    }

    static RequestSpecification as(String user) {
        return given().auth().preemptive().basic(user, user + "-pw");
    }

    static RequestSpecification anonymous() {
        return given();
    }
}
