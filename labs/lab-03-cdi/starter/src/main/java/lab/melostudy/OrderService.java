package lab.melostudy;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Registra pedidos.
 *
 * TODO (Tarea 3): la hora debe venir de un java.time.Clock inyectado, no de Instant.now().
 * TODO (Tarea 4): auditar place() con un interceptor.
 * TODO (Tarea 5): disparar OrderPlaced (síncrono y asíncrono) al registrar el pedido.
 */
@ApplicationScoped
public class OrderService {

    private final AtomicLong sequence = new AtomicLong();

    public Order place(String item, int quantity) {
        return new Order(sequence.incrementAndGet(), item, quantity, Instant.now());
    }
}
