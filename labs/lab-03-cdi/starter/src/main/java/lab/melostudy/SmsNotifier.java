package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

// TODO (Tarea 2): necesita un qualifier para distinguirse de EmailNotifier
@ApplicationScoped
public class SmsNotifier implements Notifier {

    @Override
    public String send(String to, String message) {
        return "sms:" + to + ":" + message;
    }
}
