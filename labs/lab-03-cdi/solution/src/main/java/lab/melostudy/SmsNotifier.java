package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

@Channel(Channel.Type.SMS)
@ApplicationScoped
public class SmsNotifier implements Notifier {

    @Override
    public String send(String to, String message) {
        return "sms:" + to + ":" + message;
    }
}
