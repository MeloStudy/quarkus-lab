package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/** Task 1: where does each endpoint run? The framework guesses from the return type; three of these guesses are not what the code needs. */
@QuarkusTest
@TestProfile(ExecutionProfile.class)
class ThreadKindTest extends ExecutionTestBase {

    static Stream<Arguments> endpoints() {
        return Stream.of(
                Arguments.of("plain", "WORKER", "returns an object, no annotation"),
                Arguments.of("uni", "EVENT_LOOP", "returns a Uni, no annotation"),
                Arguments.of("legacy", "WORKER", "returns a Uni but calls a blocking driver"),
                Arguments.of("cheap", "EVENT_LOOP", "returns an object but never waits"),
                Arguments.of("virtual", "VIRTUAL", "blocks, and many requests may wait at once"));
    }

    @ParameterizedTest(name = "/where/{0} runs on {1} ({2})")
    @MethodSource("endpoints")
    void eachEndpointRunsWhereItNeedsToRun(String path, String kind, String why) {
        given().get("/where/" + path).then().statusCode(200).body("kind", is(kind));
    }

    @ParameterizedTest(name = "/where/{0}: blocking is allowed exactly when it is not the event loop")
    @MethodSource("endpoints")
    void theFrameworkKnowsWhetherBlockingIsAllowed(String path, String kind, String why) {
        boolean allowed = given().get("/where/" + path).then().statusCode(200).extract().path("blockingAllowed");

        assertEquals(!"EVENT_LOOP".equals(kind), allowed);
    }

    @ParameterizedTest(name = "/where/{0} names its thread")
    @MethodSource("endpoints")
    void theThreadNameTellsTheSameStory(String path, String kind, String why) {
        String expectedStart = switch (kind) {
            case "EVENT_LOOP" -> "vert.x-eventloop-thread-";
            case "VIRTUAL" -> "quarkus-virtual-thread-";
            default -> "executor-thread-";
        };

        given().get("/where/" + path).then().statusCode(200).body("thread", startsWith(expectedStart));
    }
}
