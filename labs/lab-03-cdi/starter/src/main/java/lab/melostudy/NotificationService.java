package lab.melostudy;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Envía notificaciones por canal.
 *
 * TODO (Tarea 2): inyecta los notifiers por constructor.
 * - emailTo / smsTo usan UN canal concreto (qualifier).
 * - broadcast usa TODAS las implementaciones de Notifier (Instance<Notifier> con @Any).
 */
@ApplicationScoped
public class NotificationService {

    public String emailTo(String to, String message) {
        throw new UnsupportedOperationException("TODO");
    }

    public String smsTo(String to, String message) {
        throw new UnsupportedOperationException("TODO");
    }

    /** Devuelve un acuse por cada canal disponible, ordenados alfabéticamente. */
    public List<String> broadcast(String to, String message) {
        throw new UnsupportedOperationException("TODO");
    }
}
