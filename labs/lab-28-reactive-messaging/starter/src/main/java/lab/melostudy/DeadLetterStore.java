package lab.melostudy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

/** The end of the {@code dead-letters} channel: a list in memory (a real service would write a table or a topic). */
@ApplicationScoped
public class DeadLetterStore {

    private final List<DeadLetter> letters = new CopyOnWriteArrayList<>();

    // TODO task 3: this is the end of the "dead-letters" channel: make it a consumer of that channel.
    public void keep(DeadLetter letter) {
        letters.add(letter);
    }

    public List<DeadLetter> all() {
        return List.copyOf(letters);
    }

    public void reset() {
        letters.clear();
    }
}
