package lab.tracker;

/** Optional criteria of the issue list: null means "do not filter by this". */
public record IssueFilter(String project, Status status, String assignee, Priority priority, String label,
        String text) {
}
