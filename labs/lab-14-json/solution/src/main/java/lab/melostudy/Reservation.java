package lab.melostudy;

import java.time.Instant;
import java.time.LocalDate;

/** A stored reservation as the API shows it. {@code createdAt} is a point in time (UTC), the dates are calendar days. */
public record Reservation(
        long id,
        String guestName,
        LocalDate checkIn,
        LocalDate checkOut,
        RoomType roomType,
        int nights,
        Money total,
        Money deposit,
        Payment payment,
        Instant createdAt) {
}
