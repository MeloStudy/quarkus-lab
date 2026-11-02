package lab.melostudy;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;

/** Identifica la petición HTTP en curso: cada petición debe tener un id distinto. */
@RequestScoped
public class RequestContextInfo {

    private final String id = UUID.randomUUID().toString();

    public String id() {
        return id;
    }
}
