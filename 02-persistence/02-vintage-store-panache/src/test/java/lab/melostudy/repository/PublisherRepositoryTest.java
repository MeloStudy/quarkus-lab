package lab.melostudy.repository;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.persistence.EntityNotFoundException;
import lab.melostudy.model.Publisher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class PublisherRepositoryTest {

    @Test
    @TestTransaction
    void shouldCreateAndFindAPublisher() {
        long count = Publisher.count();
        int listAll = Publisher.listAll().size();
        assertEquals(count, listAll);

        // Creates a Publisher
        Publisher publisher = new Publisher("name");
        Publisher.persist(publisher);
        assertNotNull(publisher.id);

        assertEquals(count + 1, Publisher.count());

        // Gets the Publisher
        publisher = Publisher.findById(publisher.id);
        assertEquals("name", publisher.name);


        // Gets the Publisher by name
        publisher = Publisher.findByName(publisher.name).orElseThrow(EntityNotFoundException::new);
        assertEquals("name", publisher.name);
        assertFalse(Publisher.findContainingName("name").isEmpty());

        // Deletes the Artist
        Publisher.deleteById(publisher.id);
        assertEquals(count, Publisher.count());
    }
}