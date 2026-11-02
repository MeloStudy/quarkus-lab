package lab.melostudy;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A label shared by many products (a product has many labels, a label has many products).
 * The owning side of the relationship is {@link Product#labels}; this entity does not know its products.
 */
@Entity
@Table(name = "label")
public class Label extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 30)
    public String name;

    public static Label named(String name) {
        Label label = new Label();
        label.name = name;
        return label;
    }

    public static Optional<Label> findByName(String name) {
        return find("name", name).firstResultOptional();
    }
}
