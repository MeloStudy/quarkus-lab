package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HitReader {

    private final HitCounter counter;

    HitReader(HitCounter counter) {
        this.counter = counter;
    }

    public int hits() {
        return counter.value();
    }
}
