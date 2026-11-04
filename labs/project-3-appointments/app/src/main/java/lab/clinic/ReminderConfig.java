package lab.clinic;

import java.time.Duration;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** How reminders behave. Injected and validated as one object, so a typo in a duration stops the startup (lab 2). */
@ConfigMapping(prefix = "clinic.reminders")
public interface ReminderConfig {

    /** How often the job looks for reminders that are due. */
    @WithDefault("30s")
    Duration pollInterval();

    /** A reminder is due this long before the appointment. An appointment closer than that is reminded at once. */
    @WithDefault("24h")
    Duration lead();

    /** How many reminders one tick of the job picks up. */
    @WithDefault("20")
    int batchSize();

    /** After this many failed attempts a reminder stops being retried and waits for a human (FAILED). */
    @WithDefault("5")
    int maxAttempts();

    /** The wait after the first failure; it doubles with each one (1 min, 2, 4, 8...). */
    @WithDefault("1m")
    Duration backoff();

    /** How long a picked reminder is ours: nobody else picks it, and if we crash it comes back after this. */
    @WithDefault("2m")
    Duration lease();

    /** How long a reminder is put off when the provider is known to be down (the breaker is open): not its fault. */
    @WithDefault("30s")
    Duration providerDownPause();
}
