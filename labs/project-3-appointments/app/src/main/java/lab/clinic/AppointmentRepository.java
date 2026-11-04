package lab.clinic;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

/**
 * Database access for appointments. The list query is built from FIXED fragments: user input only ever travels as
 * a named parameter, never as part of the query text (lab 10). The three associations of a row are fetched in the
 * same query (they are to-one, so the page is still cut in the database): no N+1 (lab 9).
 */
@ApplicationScoped
public class AppointmentRepository implements PanacheRepository<Appointment> {

    /** All optional; {@code from} is inclusive and {@code to} exclusive. */
    public record Filter(Long practitionerId, Long patientId, AppointmentStatus status, Instant from, Instant to) {
    }

    public record Page(List<Appointment> items, long total) {
    }

    private static final String FETCH = "from Appointment a join fetch a.practitioner join fetch a.patient join fetch a.treatment";

    public Optional<Appointment> findDetailed(long id) {
        return find(FETCH + " where a.id = ?1", id).firstResultOptional();
    }

    /** The BOOKED appointments of a practitioner that overlap {@code [from, to)}. */
    public List<Appointment> booked(long practitionerId, Instant from, Instant to) {
        return list("practitioner.id = ?1 and status = ?2 and startsAt < ?3 and endsAt > ?4", practitionerId,
                AppointmentStatus.BOOKED, to, from);
    }

    public Page page(Filter f, int page, int size) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (f.practitionerId() != null) {
            where.append(" and a.practitioner.id = :practitioner");
            params.put("practitioner", f.practitionerId());
        }
        if (f.patientId() != null) {
            where.append(" and a.patient.id = :patient");
            params.put("patient", f.patientId());
        }
        if (f.status() != null) {
            where.append(" and a.status = :status");
            params.put("status", f.status());
        }
        if (f.from() != null) {
            where.append(" and a.startsAt >= :from");
            params.put("from", f.from());
        }
        if (f.to() != null) {
            where.append(" and a.startsAt < :to");
            params.put("to", f.to());
        }
        TypedQuery<Long> count = getEntityManager().createQuery("select count(a) from Appointment a" + where, Long.class);
        params.forEach(count::setParameter);
        long total = count.getSingleResult();
        // id breaks ties, so two pages never repeat or skip a row
        List<Appointment> items = find(FETCH + where + " order by a.startsAt asc, a.id asc", params).page(page, size)
                .list();
        return new Page(items, total);
    }
}
