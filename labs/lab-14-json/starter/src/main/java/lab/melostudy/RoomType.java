package lab.melostudy;

import java.math.BigDecimal;

/**
 * The kind of room.
 * TODO (task 2): the API contract uses the codes "single", "double" and "suite". Today the JSON shows the Java
 * constant names. The Java names must be free to change without breaking a client.
 */
public enum RoomType {
    SINGLE("80.00"),
    DOUBLE("120.00"),
    SUITE("250.00");

    private final BigDecimal nightlyRate;

    RoomType(String nightlyRate) {
        this.nightlyRate = new BigDecimal(nightlyRate);
    }

    public BigDecimal nightlyRate() {
        return nightlyRate;
    }
}
