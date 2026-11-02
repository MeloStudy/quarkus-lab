package lab.melostudy;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

/**
 * A document. {@code version} is JPA optimistic locking: Hibernate adds {@code WHERE version = ?} to every UPDATE and
 * increments it. Over HTTP the same number is the ETag of the representation.
 */
@Entity
public class Document extends PanacheEntity {

    @Column(nullable = false, length = 200)
    public String title;

    @Column(nullable = false, length = 10000)
    public String content;

    @Version
    public long version;
}
