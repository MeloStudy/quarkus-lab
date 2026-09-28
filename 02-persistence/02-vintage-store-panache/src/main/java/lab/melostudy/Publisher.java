package lab.melostudy;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.time.Instant;

@Entity
public class Publisher extends PanacheEntity {
    public String name;
    public Instant createdDate = Instant.now();

    public Publisher(String name) {
        this.name = name;
    }
}

