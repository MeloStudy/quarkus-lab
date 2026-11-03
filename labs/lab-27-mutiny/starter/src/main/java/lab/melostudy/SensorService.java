package lab.melostudy;

import java.time.Duration;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

/** TODO Task 4: a sensor that reports every millisecond, whether anybody is ready for it or not. */
@ApplicationScoped
public class SensorService {

    public Multi<Long> readings() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(1));
    }
}
