package lab.melostudy.repository;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lab.melostudy.Artist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ArtistRepositoryTest {

    @Inject
    ArtistRepository repository;

    @Test
    @TestTransaction
    void shouldCreateAndFindAnArtist() {
        // basic panache queries
        long count = repository.count();
        int listAll = repository.listAll().size();
        assertEquals(count, listAll);

        // Creates an Artist
        Artist artist = new Artist("name");
        repository.persist(artist);
        assertNotNull(artist.getId());

        assertEquals(count + 1, repository.count());

        // Gets the Artists
        artist = repository.findById(artist.getId());
        assertEquals("name", artist.getName());

        // Deletes the Artist
        repository.deleteById(artist.getId());
        assertEquals(count, repository.count());
    }
}