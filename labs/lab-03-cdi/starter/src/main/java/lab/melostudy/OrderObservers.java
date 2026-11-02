package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Reacciones a OrderPlaced.
 *
 * TODO (Tarea 5): convierte estos métodos en observers.
 * - shippingPrepared y stockReserved son síncronos; el stock debe ejecutarse ANTES que el envío
 *   aunque esté declarado después (usa prioridad).
 * - customerNotified es asíncrono.
 */
@ApplicationScoped
public class OrderObservers {

    private final EventLog log;

    OrderObservers(EventLog log) {
        this.log = log;
    }

    void shippingPrepared(OrderPlaced event) {
        log.record("shipping:" + event.order().item());
    }

    void stockReserved(OrderPlaced event) {
        log.record("stock:" + event.order().item());
    }

    void customerNotified(OrderPlaced event) {
        log.record("notify:" + event.order().item());
    }
}
