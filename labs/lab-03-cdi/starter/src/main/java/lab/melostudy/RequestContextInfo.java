package lab.melostudy;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/** Identifica la petición HTTP en curso: cada petición debe tener un id distinto. */
// TODO (Tarea 1): ¿qué scope hace que el id cambie en cada petición?
@ApplicationScoped
public class RequestContextInfo {

    private final String id = UUID.randomUUID().toString();

    public String id() {
        return id;
    }
}
