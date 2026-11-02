package lab.melostudy;

import java.time.LocalDate;

/** What a client sends to create a reservation. */
public record ReservationRequest(
        String guestName,
        LocalDate checkIn,
        LocalDate checkOut,
        RoomType roomType,
        Money deposit,
        Payment payment) {
}
