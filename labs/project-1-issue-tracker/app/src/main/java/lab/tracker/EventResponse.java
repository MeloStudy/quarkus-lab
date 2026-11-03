package lab.tracker;

import java.time.Instant;

public record EventResponse(EventKind kind, String detail, Instant at) {

    public static EventResponse from(IssueEvent e) {
        return new EventResponse(e.kind, e.detail, e.createdAt);
    }
}
