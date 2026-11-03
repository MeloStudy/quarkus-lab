package lab.tracker;

import java.time.Instant;
import java.util.List;

/**
 * The API shape of an issue. A record on purpose, built by hand from the entity (no mapping library for six
 * small mappings): it must be called INSIDE the transaction, because it reads lazy data (project, labels).
 */
public record IssueResponse(
        String key,
        String project,
        String title,
        String description,
        IssueType type,
        Priority priority,
        Status status,
        String assignee,
        List<String> labels,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public static IssueResponse from(Issue i) {
        return new IssueResponse(i.key().toString(), i.project.code, i.title, i.description, i.type, i.priority,
                i.status, i.assignee, i.labels.stream().sorted().toList(), i.version, i.createdAt, i.updatedAt);
    }
}
