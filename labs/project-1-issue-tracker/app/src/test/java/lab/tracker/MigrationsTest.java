package lab.tracker;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * The schema belongs to Flyway and the database enforces what it can. Each refusal below is raw SQL, so the API
 * is bypassed on purpose: this is what protects the data from a script, a bug or another service.
 */
@QuarkusTest
class MigrationsTest {

    @Inject
    Flyway flyway;

    @Inject
    TestData data;

    @BeforeEach
    void aProject() {
        data.reset();
        data.execute("INSERT INTO project (code, name, created_at) VALUES ('TRK', 'Tracker', now())");
    }

    @Test
    void bothMigrationsRanAndNothingIsPending() {
        MigrationInfo[] applied = flyway.info().applied();
        assertArrayEquals(new String[] { "1", "2" },
                Arrays.stream(applied).map(m -> m.getVersion().getVersion()).toArray(String[]::new));
        assertEquals(0, flyway.info().pending().length);
    }

    private void insertIssue(String number, String priority, String status, String assignee) {
        long project = data.idOfProject("TRK");
        data.execute("INSERT INTO issue (project_id, number, title, type, priority, status, assignee, version, created_at, updated_at) "
                + "VALUES (" + project + ", " + number + ", 't', 'BUG', '" + priority + "', '" + status + "', "
                + (assignee == null ? "NULL" : "'" + assignee + "'") + ", 0, now(), now())");
    }

    @Test
    void theBaselineInsertIsValid() {
        insertIssue("1", "LOW", "OPEN", null);
        assertEquals(1, data.count("issue"));
    }

    @Test
    void theDatabaseRefusesWhatTheApiWouldNeverSend() {
        insertIssue("1", "LOW", "OPEN", null); // the baseline works, so the refusals below are about the values

        assertThrows(Exception.class, () -> insertIssue("2", "URGENT", "OPEN", null));          // unknown priority
        assertThrows(Exception.class, () -> insertIssue("3", "LOW", "DONE", null));             // unknown status
        assertThrows(Exception.class, () -> insertIssue("4", "LOW", "IN_PROGRESS", null));      // started without owner
        assertThrows(Exception.class, () -> insertIssue("0", "LOW", "OPEN", null));             // numbers start at 1
        assertThrows(Exception.class, () -> insertIssue("1", "HIGH", "OPEN", null));            // same key twice
        assertEquals(1, data.count("issue"));
    }

    @Test
    void projectCodesAndLabelsAreCheckedToo() {
        assertThrows(Exception.class,
                () -> data.execute("INSERT INTO project (code, name, created_at) VALUES ('lower', 'x', now())"));
        assertThrows(Exception.class,
                () -> data.execute("INSERT INTO project (code, name, created_at) VALUES ('TRK', 'twice', now())"));
        insertIssue("1", "LOW", "OPEN", null);
        assertThrows(Exception.class,
                () -> data.execute("INSERT INTO issue_label (issue_id, label) SELECT id, 'Not Valid' FROM issue"));
    }
}
