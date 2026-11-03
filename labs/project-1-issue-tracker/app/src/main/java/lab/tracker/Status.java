package lab.tracker;

import java.util.EnumSet;
import java.util.Set;

/**
 * The life of an issue. The allowed moves live here, in one place, as a pure function: it can be unit-tested
 * without Quarkus and read in ten seconds.
 *
 * <pre>
 *   OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
 *     ^____________|           |
 *     ^________________________|   (back to OPEN: reopened)
 * </pre>
 */
public enum Status {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED;

    public Set<Status> next() {
        return switch (this) {
            case OPEN -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(OPEN, RESOLVED);
            case RESOLVED -> EnumSet.of(CLOSED, OPEN);
            case CLOSED -> EnumSet.noneOf(Status.class);
        };
    }

    public boolean canMoveTo(Status target) {
        return next().contains(target);
    }
}
