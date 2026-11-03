package lab.tracker;

import java.time.Instant;

public record ProjectResponse(String code, String name, Instant createdAt) {

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.code, p.name, p.createdAt);
    }
}
