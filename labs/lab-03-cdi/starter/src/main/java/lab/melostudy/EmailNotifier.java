package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

// TODO (Tarea 2): necesita un qualifier para distinguirse de SmsNotifier
@ApplicationScoped
public class EmailNotifier implements Notifier {

    @Override
    public String send(String to, String message) {
        return "email:" + to + ":" + message;
    }
}
