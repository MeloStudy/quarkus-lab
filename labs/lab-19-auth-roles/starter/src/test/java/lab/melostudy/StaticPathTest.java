package lab.melostudy;

import static lab.melostudy.Users.anonymous;
import static lab.melostudy.Users.as;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 4: protection by PATH, for what no annotation can reach (a static file under /internal/). */
@QuarkusTest
class StaticPathTest {

    @Test
    void membersReadTheHandbookAndEverybodyElseDoesNot() {
        as("bob").get("/internal/handbook.html").then().statusCode(200).body(containsString("Staff handbook"));
        as("alice").get("/internal/handbook.html").then().statusCode(200);

        anonymous().get("/internal/handbook.html").then().statusCode(401);
        as("dave").get("/internal/handbook.html").then().statusCode(403); // authenticated, but no role
    }

    @Test
    void theCheckHappensBeforeRoutingSoAnAnonymousVisitorCannotProbeForFiles() {
        as("bob").get("/internal/handbook.html").then().statusCode(200);

        // the file does not exist, yet an anonymous visitor gets 401 (never "404", which would say "no such file")
        anonymous().get("/internal/secret-plans.html").then().statusCode(401);
        // somebody allowed in finds out that it does not exist
        as("bob").get("/internal/secret-plans.html").then().statusCode(404);
    }

    @Test
    void whatIsOutsideThePolicyIsNotAffected() {
        anonymous().get("/public/status").then().statusCode(200);
        anonymous().get("/nothing-here").then().statusCode(404); // no policy, no endpoint: plain 404
    }
}
