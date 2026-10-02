package lab.melostudy.model.cardinality;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lab.melostudy.model.inheritance.CD;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "t_tracks")
public class Track extends PanacheEntity {

    @Column(nullable = false)
    public String title;

    @Column(nullable = false)
    public Duration duration;

    @ManyToOne
    @JoinColumn(name = "cd_fk") // redefine column name
    public CD cd;

    @Column(name = "created_date", nullable = false)
    public Instant createdDate = Instant.now();
}