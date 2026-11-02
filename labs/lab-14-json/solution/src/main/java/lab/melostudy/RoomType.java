package lab.melostudy;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The kind of room. The JSON value is a lower-case code that is part of the API contract and has nothing to
 * do with the Java constant: renaming {@code SUITE} in the code must not break a single client.
 */
public enum RoomType {
    SINGLE("single", "80.00"),
    DOUBLE("double", "120.00"),
    SUITE("suite", "250.00");

    private final String code;
    private final BigDecimal nightlyRate;

    RoomType(String code, String nightlyRate) {
        this.code = code;
        this.nightlyRate = new BigDecimal(nightlyRate);
    }

    @JsonValue
    public String code() {
        return code;
    }

    public BigDecimal nightlyRate() {
        return nightlyRate;
    }
}
