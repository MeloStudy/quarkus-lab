package lab.melostudy;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Sends notifications per channel.
 *
 * TODO (Task 2): inject the notifiers through the constructor.
 * - emailTo / smsTo use ONE specific channel (qualifier).
 * - broadcast uses ALL Notifier implementations (Instance<Notifier> with @Any).
 */
@ApplicationScoped
public class NotificationService {

    public String emailTo(String to, String message) {
        throw new UnsupportedOperationException("TODO");
    }

    public String smsTo(String to, String message) {
        throw new UnsupportedOperationException("TODO");
    }

    /** Returns one acknowledgement per available channel, sorted alphabetically. */
    public List<String> broadcast(String to, String message) {
        throw new UnsupportedOperationException("TODO");
    }
}
