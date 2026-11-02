package lab.melostudy;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A stored reservation as the API shows it.
 * TODO (task 1): {@code createdAt} is a point in time that other systems in other time zones will read.
 */
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
        LocalDateTime createdAt) {
}
