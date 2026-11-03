package lab.payments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Where payments live. In memory on purpose (see the review): every method is {@code synchronized}, which makes
 * "reserve this idempotency key" one atomic step, the same guarantee a unique index gives in a database.
 */
@ApplicationScoped
public class PaymentStore {

    /** The outcome of {@link #reserve}: the payment that now owns the key, and whether it is the one just offered. */
    public record Reservation(Payment payment, boolean created) {
    }

    private final Map<String, Payment> byId = new HashMap<>();
    private final Map<String, String> idByKey = new HashMap<>();

    private static String compositeKey(String customer, String idempotencyKey) {
        return customer + '\u0000' + idempotencyKey;
    }

    /** Stores the candidate unless this customer already used this key; either way returns whoever owns the key. */
    public synchronized Reservation reserve(Payment candidate) {
        String composite = compositeKey(candidate.customer(), candidate.idempotencyKey());
        String existingId = idByKey.get(composite);
        if (existingId != null) {
            return new Reservation(byId.get(existingId), false);
        }
        idByKey.put(composite, candidate.id());
        byId.put(candidate.id(), candidate);
        return new Reservation(candidate, true);
    }

    /** Forgets a payment that never reached the provider, so that the key can be used again. */
    public synchronized void release(Payment payment) {
        byId.remove(payment.id());
        idByKey.remove(compositeKey(payment.customer(), payment.idempotencyKey()));
    }

    public synchronized Payment update(Payment payment) {
        byId.put(payment.id(), payment);
        return payment;
    }

    public synchronized Optional<Payment> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** Newest first. {@code customer == null} means everybody's payments; {@code status == null} means any status. */
    public synchronized List<Payment> list(String customer, PaymentStatus status) {
        List<Payment> result = new ArrayList<>();
        for (Payment payment : byId.values()) {
            boolean mine = customer == null || customer.equals(payment.customer());
            boolean wanted = status == null || status == payment.status();
            if (mine && wanted) {
                result.add(payment);
            }
        }
        result.sort(Comparator.comparing(Payment::createdAt).reversed().thenComparing(Payment::id));
        return result;
    }

    public synchronized long count(PaymentStatus status) {
        return byId.values().stream().filter(p -> p.status() == status).count();
    }

    /** Only the tests call this, to start every test from nothing. */
    public synchronized void clear() {
        byId.clear();
        idByKey.clear();
    }
}
