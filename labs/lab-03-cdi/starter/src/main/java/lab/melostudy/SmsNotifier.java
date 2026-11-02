package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

// TODO (Task 2): needs a qualifier to tell it apart from EmailNotifier
@ApplicationScoped
public class SmsNotifier implements Notifier {

    @Override
    public String send(String to, String message) {
        return "sms:" + to + ":" + message;
    }
}
