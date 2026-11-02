package lab.melostudy;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/** Identifies the current HTTP request: every request must get a different id. */
// TODO (Task 1): which scope makes the id change on every request?
@ApplicationScoped
public class RequestContextInfo {

    private final String id = UUID.randomUUID().toString();

    public String id() {
        return id;
    }
}
