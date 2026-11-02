package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

// TODO (Task 2): needs a qualifier to tell it apart from SmsNotifier
@ApplicationScoped
public class EmailNotifier implements Notifier {

    @Override
    public String send(String to, String message) {
        return "email:" + to + ":" + message;
    }
}
