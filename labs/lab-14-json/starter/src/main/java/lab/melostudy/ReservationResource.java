package lab.melostudy;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Reservations, kept in memory: this lab is about the JSON, not about storage. */
@Path("/reservations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReservationResource {

    private final Map<Long, Reservation> store = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    @POST
    public RestResponse<Reservation> create(ReservationRequest request) {
        if (request == null || request.guestName() == null || request.guestName().isBlank()
                || request.checkIn() == null || request.checkOut() == null || request.roomType() == null
                || !request.checkOut().isAfter(request.checkIn())) {
            throw new BadRequestException("guest name, dates (check-out after check-in) and room type are required");
        }
        int nights = (int) ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        Money total = new Money(request.roomType().nightlyRate(), "EUR").times(nights);
        Reservation reservation = new Reservation(ids.incrementAndGet(), request.guestName(), request.checkIn(),
                request.checkOut(), request.roomType(), nights, total, request.deposit(), request.payment(),
                LocalDateTime.now());
        store.put(reservation.id(), reservation);
        return RestResponse.ResponseBuilder.<Reservation>created(URI.create("/reservations/" + reservation.id()))
                .entity(reservation).build();
    }

    @GET
    @Path("/{id}")
    public Reservation get(@RestPath long id) {
        Reservation reservation = store.get(id);
        if (reservation == null) {
            throw new NotFoundException();
        }
        return reservation;
    }
}
