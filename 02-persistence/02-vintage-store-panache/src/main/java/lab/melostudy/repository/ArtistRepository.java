package lab.melostudy.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lab.melostudy.Artist;

@ApplicationScoped
public class ArtistRepository implements PanacheRepository<Artist> {

}