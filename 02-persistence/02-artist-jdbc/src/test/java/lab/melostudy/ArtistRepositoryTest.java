package lab.melostudy;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ArtistRepositoryTest {

    @Inject
    ArtistRepository repository;

    @Test
    void shouldCreateAndFindAnArtist() throws SQLException {
        Artist artist = new Artist("name");

        repository.persist(artist);
        assertNotNull(artist.getId());

        artist = repository.findById(artist.getId());
        assertEquals("name", artist.getName());
    }
}