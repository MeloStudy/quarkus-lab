package lab.melostudy.model.cardinality;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lab.melostudy.Customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "t_purchase_orders")
public class PurchaseOrder extends PanacheEntity {

    @Column(name = "purchase_order_date", nullable = false)
    public LocalDate date = LocalDate.now();

    // to avoid override column definition
    // and not force to use join table t_purchase_orders_t_purchase_order_lines
    @OneToMany(mappedBy = "purchaseOrder")
    public List<OrderLine> orderLines = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "customer_fk") // redefine column name
    public Customer customer;

    @Column(name = "created_date", nullable = false)
    public Instant createdDate = Instant.now();

    public void addOrderLine(OrderLine orderLine) {
        orderLines.add(orderLine);
        orderLine.purchaseOrder = this;
    }
}