package lab.melostudy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

/** In-memory storage: the lab is about the token, not about persistence. */
@ApplicationScoped
public class NoteStore {

    private final Map<Long, Note> notes = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public Note add(String author, String title) {
        long id = ids.incrementAndGet();
        Note note = new Note(id, author, title);
        notes.put(id, note);
        return note;
    }

    public List<Note> all() {
        return notes.values().stream().sorted((a, b) -> Long.compare(a.id(), b.id())).toList();
    }

    public Optional<Note> find(long id) {
        return Optional.ofNullable(notes.get(id));
    }

    public boolean remove(long id) {
        return notes.remove(id) != null;
    }

    /** Only the tests call this, to start every test from an empty store. */
    public void clear() {
        notes.clear();
    }
}
