package lab.clinic;

import java.time.ZoneId;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * The clinic's settings. One time zone for the whole service: working hours are LOCAL times of this zone and the
 * API shows every instant in it too. Validated and injected as one object, so a typo in the zone fails at startup
 * (lab 2, lab 22) and not on the first request.
 */
@ConfigMapping(prefix = "clinic")
public interface ClinicConfig {

    ZoneId timeZone();

    /** Appointments start on a grid of this many minutes (local time). */
    @WithDefault("15")
    int slotMinutes();

    /** How far ahead an appointment may be booked. */
    @WithDefault("90")
    int horizonDays();
}
