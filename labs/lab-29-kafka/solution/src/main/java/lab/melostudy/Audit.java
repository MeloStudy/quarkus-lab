package lab.melostudy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A second reader of the same topic with its own consumer group ({@code audit}): it sees every order, whatever shipping does
 * with it. It counts orders per customer.
 */
@ApplicationScoped
public class Audit {

    private final Map<String, AtomicInteger> perCustomer = new ConcurrentHashMap<>();

    @Incoming("audit")
    public void count(Order order) {
        if (order == null) {
            return;
        }
        perCustomer.computeIfAbsent(order.customer(), customer -> new AtomicInteger()).incrementAndGet();
    }

    public int countOf(String customer) {
        AtomicInteger count = perCustomer.get(customer);
        return count == null ? 0 : count.get();
    }
}
