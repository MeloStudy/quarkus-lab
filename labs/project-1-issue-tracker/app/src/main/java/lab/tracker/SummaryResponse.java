package lab.tracker;

import java.util.Map;

/**
 * Counts for a dashboard. Every status and priority appears, with 0 when there is none: a client should not have
 * to guess that a missing key means zero.
 *
 * @param byStatus   all the issues of the project
 * @param byPriority only the issues that are not CLOSED (the work that is still to do)
 */
public record SummaryResponse(String project, long total, Map<Status, Long> byStatus, Map<Priority, Long> byPriority) {
}
