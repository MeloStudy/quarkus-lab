package lab.melostudy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/** Task 2: one id per request, in the response and in every line the request writes. */
@QuarkusTest
@TestProfile(LogProfile.class)
class RequestIdTest extends LabTestBase {

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Test
    void aSafeIdFromTheCallerIsKeptAndEchoed() {
        order("A1", "client-abc.123_X").then().statusCode(201).header("X-Request-Id", is("client-abc.123_X"));
    }

    @Test
    void withoutAnIdOneIsGeneratedAndEchoed() {
        String id = order("A1").then().statusCode(201).extract().header("X-Request-Id");
        assertThat(id, matchesPattern(UUID_PATTERN));
    }

    @Test
    void anUnsafeIdIsReplacedAndNeverLogged() {
        int mark = Logs.mark();
        String tooLong = "a".repeat(65);
        for (String unsafe : List.of("two words", "<script>x</script>", "semi;colon", tooLong)) {
            String id = order("A1", unsafe).then().statusCode(201).extract().header("X-Request-Id");
            assertThat("replaced: " + unsafe, id, matchesPattern(UUID_PATTERN));
        }
        Logs.await(mark, r -> Logs.app(r).size() >= 8);
        String log = String.join("\n", Logs.rawLinesSince(mark));
        assertThat(log, not(containsString("<script>")));
        assertThat(log, not(containsString("two words")));
        assertThat(log, not(containsString(tooLong)));
    }

    @Test
    void everyLineOfTheRequestCarriesTheId() {
        int mark = Logs.mark();
        order("A1", "trace-me-1").then().statusCode(201);

        List<JsonNode> lines = Logs.app(Logs.await(mark, r -> Logs.app(r).size() >= 2));
        assertThat(lines.size(), greaterThanOrEqualTo(2));
        for (JsonNode line : lines) {
            if (Logs.text(line, "loggerName").endsWith("OrderService")) {
                assertThat(Logs.text(line, "message"), Logs.text(line, "requestId"), is("trace-me-1"));
            }
        }
    }

    @Test
    void requestsDoNotShareTheirIds() {
        int mark = Logs.mark();
        order("A1", "first-1").then().statusCode(201);
        order("A1", "second-2").then().statusCode(201);

        List<JsonNode> lines = Logs.app(Logs.await(mark, r -> Logs.app(r).stream()
                .filter(x -> Logs.text(x, "loggerName").endsWith("OrderService")).count() >= 4));
        List<JsonNode> orderLines = lines.stream().filter(x -> Logs.text(x, "loggerName").endsWith("OrderService")).toList();
        assertThat(orderLines.size(), is(4));
        assertThat(Logs.text(orderLines.get(0), "requestId"), is("first-1"));
        assertThat(Logs.text(orderLines.get(1), "requestId"), is("first-1"));
        assertThat(Logs.text(orderLines.get(2), "requestId"), is("second-2"));
        assertThat(Logs.text(orderLines.get(3), "requestId"), is("second-2"));
    }

    @Test
    void failuresCarryTheIdToo() {
        order("SOLD-OUT", "refused-1").then().statusCode(409).header("X-Request-Id", is("refused-1"));
        order("BROKEN", "broken-1").then().statusCode(502).header("X-Request-Id", is("broken-1"));
        request().header("X-Request-Id", "invalid-1").body("{\"sku\":\"\",\"quantity\":0}").post("/api/orders")
                .then().statusCode(400).header("X-Request-Id", is("invalid-1"));
    }

}
