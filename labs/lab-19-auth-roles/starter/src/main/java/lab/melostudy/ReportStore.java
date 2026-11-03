package lab.melostudy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

/** In-memory storage: the lab is about who may do what, not about persistence. */
@ApplicationScoped
public class ReportStore {

    private final Map<Long, Report> reports = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public Report add(String owner, String title, String body) {
        long id = ids.incrementAndGet();
        Report report = new Report(id, owner, title, body);
        reports.put(id, report);
        return report;
    }

    public Optional<Report> find(long id) {
        return Optional.ofNullable(reports.get(id));
    }

    public List<Report> all() {
        return reports.values().stream().sorted((a, b) -> Long.compare(a.id(), b.id())).toList();
    }

    public List<Report> ownedBy(String owner) {
        return all().stream().filter(r -> r.owner().equals(owner)).toList();
    }

    public boolean remove(long id) {
        return reports.remove(id) != null;
    }

    /** Only the tests call this, to start every test from an empty store. */
    public void clear() {
        reports.clear();
    }
}
