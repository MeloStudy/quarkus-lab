package lab.melostudy;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;

/** Identifies the current HTTP request: every request must get a different id. */
@RequestScoped
public class RequestContextInfo {

    private final String id = UUID.randomUUID().toString();

    public String id() {
        return id;
    }
}
