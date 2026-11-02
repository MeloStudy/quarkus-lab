package lab.melostudy;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Contador de visitas. {@code HitWriter} escribe y {@code HitReader} lee: ambos
 * deben ver el mismo contador.
 */
@ApplicationScoped
public class HitCounter {

    private final AtomicInteger value = new AtomicInteger();

    public void increment() {
        value.incrementAndGet();
    }

    public int value() {
        return value.get();
    }
}
