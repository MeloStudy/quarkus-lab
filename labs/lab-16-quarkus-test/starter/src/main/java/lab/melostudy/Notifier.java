package lab.melostudy;

import org.jboss.logging.Logger;

import jakarta.inject.Singleton;

/** Tells the customer something (an e-mail, a push...). Here it only logs. */
@Singleton
public class Notifier {

    private static final Logger LOG = Logger.getLogger(Notifier.class);

    public void send(String customerId, String message) {
        LOG.infof("notify %s: %s", customerId, message);
    }
}
