package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BookRepository {

    @ConfigProperty(name = "books.author", defaultValue = "MeloDev")
    String author;

    public List<Book> getAllBooks() {
        return List.of(
                new Book(1, "Book 1", author, 2026),
                new Book(2, "Book 2", "King", 2026),
                new Book(3, "Book 3", "Kong", 2027)
        );
    }

    public Optional<Book> getBook(int id) {
        return getAllBooks().stream()
                .filter(book -> book.id == id)
                .findFirst();
    }

}
