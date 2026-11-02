package lab.melostudy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

/** Record of what the event observers do. */
@ApplicationScoped
public class EventLog {

    private final List<String> entries = new CopyOnWriteArrayList<>();

    public void record(String entry) {
        entries.add(entry);
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    public void clear() {
        entries.clear();
    }
}
