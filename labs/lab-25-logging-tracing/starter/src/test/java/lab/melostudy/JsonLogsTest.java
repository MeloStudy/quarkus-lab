package lab.melostudy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/** Task 1: logs a machine can read. */
@QuarkusTest
@TestProfile(LogProfile.class)
class JsonLogsTest extends LabTestBase {

    @Test
    void everyLineIsAJsonObject() throws Exception {
        order("A1").then().statusCode(201);
        Logs.await(0, records -> !Logs.app(records).isEmpty());

        List<String> lines = Logs.rawLines();
        assertThat(lines, is(not(empty())));
        ObjectMapper json = new ObjectMapper();
        for (String line : lines) {
            JsonNode node = json.readTree(line);
            assertThat("not a JSON object: " + line, node.isObject(), is(true));
        }
    }

    @Test
    void recordsHaveTheFieldsASearchNeeds() {
        int mark = Logs.mark();
        order("A1").then().statusCode(201);

        List<JsonNode> records = Logs.app(Logs.await(mark, r -> Logs.app(r).size() >= 2));
        assertThat(records, is(not(empty())));
        JsonNode record = records.get(0);
        for (String field : List.of("timestamp", "level", "loggerName", "message")) {
            assertThat("missing " + field, Logs.text(record, field), is(notNullValue()));
        }
    }

    @Test
    void mdcEntriesAreTopLevelFields() {
        int mark = Logs.mark();
        MDC.put("probe", "p-1");
        try {
            Logger.getLogger("lab.melostudy.Probe").info("probe line");
        } finally {
            MDC.remove("probe");
        }

        List<JsonNode> records = Logs.await(mark, r -> r.stream().anyMatch(x -> "probe line".equals(Logs.text(x, "message"))));
        JsonNode probe = records.stream().filter(x -> "probe line".equals(Logs.text(x, "message"))).findFirst().orElseThrow();
        assertThat(Logs.text(probe, "probe"), is("p-1"));
    }

    @Test
    void theConsoleIsPlainInTestsAndFlatLikeTheFile() {
        Config config = ConfigProvider.getConfig();
        assertThat(config.getValue("quarkus.log.console.json.enabled", Boolean.class), is(false));
        assertThat(config.getValue("quarkus.log.console.json.mdc.flat-fields", Boolean.class), is(true));
    }
}
