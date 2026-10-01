package lab.melostudy.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "t_publishers")
public class Publisher extends PanacheEntity {
    @Column(length = 50, nullable = false)
    public String name;

    @Column(name = "created_date", nullable = false)
    public Instant createdDate = Instant.now();

    public Publisher(String name) {
        this.name = name;
    }
}

