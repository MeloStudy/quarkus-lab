package lab.melostudy;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultRateLoader implements RateLoader {

    @Override
    public Map<String, Double> load() {
        return Map.of("EUR", 1.0, "USD", 1.08, "PEN", 4.1);
    }
}
