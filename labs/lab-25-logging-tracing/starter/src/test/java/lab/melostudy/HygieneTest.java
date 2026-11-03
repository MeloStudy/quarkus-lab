package lab.melostudy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/** Task 5: logs that are safe to keep and honest about what went wrong. */
@QuarkusTest
@TestProfile(LogProfile.class)
class HygieneTest extends LabTestBase {

    private static boolean isWarning(JsonNode record) {
        String level = Logs.text(record, "level");
        return "WARN".equals(level) || "ERROR".equals(level);
    }

    @Test
    void theCustomersEmailNeverReachesTheLog() {
        int mark = Logs.mark();
        order("A1", "pii-ok").then().statusCode(201);
        order("SOLD-OUT", "pii-refused").then().statusCode(409);
        order("BROKEN", "pii-broken").then().statusCode(502);
        Logs.await(mark, r -> Logs.app(r).size() >= 8);

        String log = String.join("\n", Logs.rawLinesSince(mark));
        assertThat("the lines of the first order are in the log", log, containsString("pii-ok"));
        assertThat(log, not(containsString(EMAIL)));
        assertThat(log, not(containsString("example.com")));
    }

    @Test
    void aRefusalIsNotAWarning() {
        order("A1", "warm-up").then().statusCode(201);
        int mark = Logs.mark();
        order("SOLD-OUT", "refused-2").then().statusCode(409);

        List<JsonNode> lines = Logs.app(Logs.await(mark, r -> !Logs.app(r).isEmpty()));
        assertThat("the request wrote no log lines at all", lines, is(not(empty())));
        assertThat(lines.stream().filter(HygieneTest::isWarning).toList(), is(empty()));
    }

    @Test
    void aFailureOfTheStockServiceLeavesAWarningThatPointsAtTheRequest() {
        int mark = Logs.mark();
        order("BROKEN", "broken-2").then().statusCode(502);

        List<JsonNode> lines = Logs.await(mark, r -> r.stream().anyMatch(HygieneTest::isWarning));
        JsonNode warning = lines.stream().filter(HygieneTest::isWarning).findFirst().orElse(null);
        assertThat("the 502 left no warning in the log", warning != null, is(true));
        assertThat(Logs.text(warning, "requestId"), is("broken-2"));
        assertThat(Logs.text(warning, "message"), containsString("500"));
    }
}
