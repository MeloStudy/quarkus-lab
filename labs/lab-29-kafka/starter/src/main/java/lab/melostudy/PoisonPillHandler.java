package lab.melostudy;

import org.apache.kafka.common.header.Headers;
import org.jboss.logging.Logger;

import io.smallrye.reactive.messaging.kafka.DeserializationFailureHandler;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * What to do with a record that cannot be turned into an {@link Order} (not JSON, wrong shape). Without a handler the consumer
 * is stuck on that record: every poll fails at the same offset, for ever. With one, the consumer moves on: the record goes on as a
 * message with a {@code null} payload, and the failure strategy of the channel decides its fate (here: the dead-letter topic,
 * with the original bytes). Linked to a channel by its identifier in {@code application.properties}.
 */
// TODO task 3: give this bean the identifier "order-poison-pill" so that a channel can use it (application.properties).
@ApplicationScoped
public class PoisonPillHandler implements DeserializationFailureHandler<Order> {

    private static final Logger LOG = Logger.getLogger(PoisonPillHandler.class);

    @Override
    public Order handleDeserializationFailure(String topic, boolean isKey, String deserializer, byte[] data, Exception exception, Headers headers) {
        LOG.warnf("Skipping a record of %s that is not an order: %s", topic, exception.getMessage());
        return null;
    }
}
