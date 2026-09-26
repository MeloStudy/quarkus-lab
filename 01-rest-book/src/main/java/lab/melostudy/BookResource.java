package lab.melostudy;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

@Path("/api/books")
public class BookResource {

    @Inject
    BookRepository bookRepository;
    @Inject
    Logger log;

    @GET
    public List<Book> getAllBooks() {
        log.info("Getting all books");
        return bookRepository.getAllBooks();
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public int countAllBooks() {
        log.info("Getting all books count");
        return bookRepository.getAllBooks().size();
    }

    @GET
    @Path("{id}")
    public Optional<Book> getBookById(@PathParam("id") Integer id) {
        log.info("Get a single book with id: " + id);
        return bookRepository.getBook(id);
    }

}
