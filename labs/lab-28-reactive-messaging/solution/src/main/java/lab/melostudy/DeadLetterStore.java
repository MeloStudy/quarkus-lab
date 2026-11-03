package lab.melostudy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;

/** The end of the {@code dead-letters} channel: a list in memory (a real service would write a table or a topic). */
@ApplicationScoped
public class DeadLetterStore {

    private final List<DeadLetter> letters = new CopyOnWriteArrayList<>();

    @Incoming("dead-letters")
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
