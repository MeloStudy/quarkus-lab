package lab.melostudy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/** Task 3: the id follows the work, to the next service and to the threads that continue it. */
@QuarkusTest
@TestProfile(LogProfile.class)
class DownstreamTest extends LabTestBase {

    private static boolean isAudit(JsonNode record) {
        return "lab.melostudy.AuditLog".equals(Logs.text(record, "loggerName"));
    }

    @BeforeEach
    void freshStub() {
        Stub.reset();
    }

    @Test
    void theStockServiceReceivesTheRequestId() {
        order("A1", "down-1").then().statusCode(201);

        List<Map<String, Object>> calls = Stub.requests();
        assertThat(calls, hasSize(1));
        assertThat(calls.get(0).get("requestId"), is("down-1"));
    }

    @Test
    void aGeneratedIdTravelsToo() {
        String id = order("A1").then().statusCode(201).extract().header("X-Request-Id");

        assertThat("the response carries a generated id", id, is(notNullValue()));
        assertThat(Stub.requests().get(0).get("requestId"), is(id));
    }

    @Test
    void theAuditLineWrittenByAnotherThreadKeepsTheId() {
        int mark = Logs.mark();
        order("A1", "audit-1").then().statusCode(201);

        List<JsonNode> records = Logs.await(mark, r -> r.stream().anyMatch(DownstreamTest::isAudit));
        JsonNode audit = records.stream().filter(DownstreamTest::isAudit).findFirst().orElse(null);
        assertThat("the audit line was not written", audit, is(notNullValue()));
        assertThat(Logs.text(audit, "message"), Logs.text(audit, "requestId"), is("audit-1"));
    }
}
