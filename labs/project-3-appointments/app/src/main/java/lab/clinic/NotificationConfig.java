package lab.clinic;

import io.smallrye.config.ConfigMapping;

/**
 * The credentials of the notification provider. No default on purpose: without {@code NOTIFICATIONS_API_KEY} the
 * application refuses to start (injected and validated at startup, labs 2 and 22), instead of failing at the first
 * reminder, days later.
 */
@ConfigMapping(prefix = "notifications")
public interface NotificationConfig {

    String apiKey();
}
