package lab.melostudy;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;

/**
 * Contador de visitas. {@code HitWriter} escribe y {@code HitReader} lee: ambos
 * deben ver el mismo contador.
 */
// TODO (Tarea 1): ¿es @Dependent el scope correcto para un estado compartido?
@Dependent
public class HitCounter {

    private final AtomicInteger value = new AtomicInteger();

    public void increment() {
        value.incrementAndGet();
    }

    public int value() {
        return value.get();
    }
}
