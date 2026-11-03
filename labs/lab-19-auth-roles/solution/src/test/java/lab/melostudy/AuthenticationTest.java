package lab.melostudy;

import static io.restassured.RestAssured.given;
import static lab.melostudy.Users.anonymous;
import static lab.melostudy.Users.as;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 1: who are you? HTTP Basic against the embedded realm. */
@QuarkusTest
class AuthenticationTest {

    @Test
    void validCredentialsGiveAnIdentity() {
        as("alice").get("/api/me").then().statusCode(200)
                .body("name", equalTo("alice"), "roles", contains("admin"));
        as("bob").get("/api/me").then().statusCode(200)
                .body("name", equalTo("bob"), "roles", contains("user"));
    }

    @Test
    void noCredentialsIs401AndTheServerAsksForBasic() {
        as("bob").get("/api/me").then().statusCode(200); // the endpoint works for a logged-in user...

        anonymous().get("/api/me").then().statusCode(401)
                .header("WWW-Authenticate", equalToIgnoringCase("basic")); // ...and asks everybody else to log in
    }

    @Test
    void aClientThatWaitsForTheChallengeGetsInToo() {
        // not preemptive: first request without header -> 401 + challenge -> second request with the header
        given().auth().basic("alice", "alice-pw").get("/api/me").then().statusCode(200).body("name", equalTo("alice"));
    }

    @Test
    void wrongPasswordUnknownUserAndGarbageAreTheSame401() {
        as("alice").get("/api/me").then().statusCode(200);

        given().auth().preemptive().basic("alice", "wrong").get("/api/me").then().statusCode(401).body(equalTo(""));
        given().auth().preemptive().basic("nobody-like-this", "x").get("/api/me").then().statusCode(401).body(equalTo(""));
        given().header("Authorization", "Basic !!not-base64!!").get("/api/me").then().statusCode(401).body(equalTo(""));
    }

    @Test
    void aUserWithoutRolesIsStillAuthenticated() {
        as("dave").get("/api/me").then().statusCode(200).body("name", equalTo("dave"), "roles", empty());
    }

    @Test
    void thePublicEndpointNeedsNoCredentials() {
        anonymous().get("/public/status").then().statusCode(200).body("status", equalTo("UP"));
    }
}
