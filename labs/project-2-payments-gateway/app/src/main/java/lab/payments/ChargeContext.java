package lab.payments;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Remembers, across the attempts of one charge, whether any of them <b>may</b> have reached the provider. The retry
 * and the circuit breaker live outside the method that calls the provider, so they cannot tell the caller; this can.
 * <p>
 * An attempt is counted <i>before</i> the call, so that a timeout that interrupts the call still counts as "sent". It
 * is un-counted only when the connection was refused: then nothing left this machine.
 */
public final class ChargeContext {

    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicInteger refused = new AtomicInteger();

    void attempting() {
        attempts.incrementAndGet();
    }

    void refused() {
        refused.incrementAndGet();
    }

    /** False when no attempt was made (breaker open) or every attempt was refused at the door. */
    public boolean mayHaveBeenProcessed() {
        return attempts.get() > refused.get();
    }
}
