package lab.melostudy.model.cardinality;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lab.melostudy.model.inheritance.Item;

import java.time.Instant;

@Entity
@Table(name = "t_purchase_order_lines")
public class OrderLine extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "item_fk")
    public Item item;

    @Column(nullable = false)
    public Integer quantity;

    @ManyToOne
    @JoinColumn(name = "purchase_order_fk") // redefine column name
    public PurchaseOrder purchaseOrder;

    @Column(name = "created_date", nullable = false)
    public Instant createdDate = Instant.now();

}