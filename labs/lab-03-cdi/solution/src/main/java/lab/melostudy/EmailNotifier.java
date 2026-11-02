package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

@Channel(Channel.Type.EMAIL)
@ApplicationScoped
public class EmailNotifier implements Notifier {

    @Override
    public String send(String to, String message) {
        return "email:" + to + ":" + message;
    }
}
