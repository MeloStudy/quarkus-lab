package lab.melostudy;

import java.util.Map;

/** Reads the exchange rates from somewhere slow and blocking (a database, a file share): every call is expensive. */
public interface RateLoader {

    Map<String, Double> load();
}
