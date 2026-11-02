package lab.melostudy;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

/** Envía notificaciones por canal. */
@ApplicationScoped
public class NotificationService {

    private final Notifier email;
    private final Notifier sms;
    private final Instance<Notifier> all;

    NotificationService(@Channel(Channel.Type.EMAIL) Notifier email,
            @Channel(Channel.Type.SMS) Notifier sms,
            @Any Instance<Notifier> all) {
        this.email = email;
        this.sms = sms;
        this.all = all;
    }

    public String emailTo(String to, String message) {
        return email.send(to, message);
    }

    public String smsTo(String to, String message) {
        return sms.send(to, message);
    }

    /** Devuelve un acuse por cada canal disponible, ordenados alfabéticamente. */
    public List<String> broadcast(String to, String message) {
        return all.stream()
                .map(notifier -> notifier.send(to, message))
                .sorted()
                .toList();
    }
}
