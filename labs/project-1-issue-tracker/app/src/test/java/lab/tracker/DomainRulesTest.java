package lab.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Plain JUnit, no Quarkus: pure rules need no container, so these run in milliseconds. The slow tests of the rest
 * of the suite should only cover what needs the application (HTTP, SQL, transactions).
 */
class DomainRulesTest {

    @Test
    void theStateMachineAsATable() {
        assertEquals(Set.of(Status.IN_PROGRESS), Status.OPEN.next());
        assertEquals(Set.of(Status.OPEN, Status.RESOLVED), Status.IN_PROGRESS.next());
        assertEquals(Set.of(Status.CLOSED, Status.OPEN), Status.RESOLVED.next());
        assertEquals(EnumSet.noneOf(Status.class), Status.CLOSED.next());
    }

    @Test
    void noStatusMovesToItself() {
        for (Status s : Status.values()) {
            assertFalse(s.canMoveTo(s), s + " -> " + s);
        }
    }

    @Test
    void closedIsReachableOnlyFromResolved() {
        for (Status s : Status.values()) {
            assertEquals(s == Status.RESOLVED, s.canMoveTo(Status.CLOSED), s + " -> CLOSED");
        }
    }

    @Test
    void priorityIsDeclaredFromLeastToMostUrgent() {
        assertTrue(Priority.LOW.compareTo(Priority.MEDIUM) < 0);
        assertTrue(Priority.HIGH.compareTo(Priority.CRITICAL) < 0);
    }

    @Test
    void aKeyIsParsedAndPrintedBack() {
        IssueKey key = IssueKey.parse("TRK-12");
        assertEquals("TRK", key.code());
        assertEquals(12, key.number());
        assertEquals("TRK-12", key.toString());
    }

    @Test
    void aKeyThatCannotExistIsNotFound() {
        for (String bad : new String[] { null, "", "TRK", "TRK-", "TRK-0", "TRK-01", "trk-1", "T-1", "TOOLONG-1",
                "TRK-1234567890", "TRK-1 ", "TRK--1" }) {
            TrackerException e = assertThrows(TrackerException.class, () -> IssueKey.parse(bad), "key: " + bad);
            assertEquals(404, e.status());
        }
    }
}
