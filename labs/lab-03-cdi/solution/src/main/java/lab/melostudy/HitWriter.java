package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HitWriter {

    private final HitCounter counter;

    HitWriter(HitCounter counter) {
        this.counter = counter;
    }

    public void hit() {
        counter.increment();
    }
}
