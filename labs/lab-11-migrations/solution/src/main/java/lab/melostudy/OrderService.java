package lab.melostudy;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

/**
 * Places orders. One order is ONE unit of work: take the stock, save the order, ask for the payment.
 * Either everything is saved or nothing is.
 */
@ApplicationScoped
public class OrderService {

    /** Orders above this total are refused by the (simulated) payment provider. */
    static final BigDecimal PAYMENT_LIMIT = new BigDecimal("500.00");

    private final ProductRepository products;
    private final AuditService audit;

    OrderService(ProductRepository products, AuditService audit) {
        this.products = products;
        this.audit = audit;
    }

    /**
     * {@code rollbackOn}: by default only unchecked exceptions roll back; {@link PaymentRefusedException}
     * is checked, so it must be listed or the stock would stay taken by an order that was refused.
     */
    @Transactional(rollbackOn = PaymentRefusedException.class)
    public CustomerOrder place(long productId, int quantity) throws PaymentRefusedException {
        // PESSIMISTIC_WRITE = SELECT ... FOR UPDATE: concurrent orders for the same product wait here,
        // one at a time, and each one sees the stock left by the previous one.
        Product product = products.findById(productId, LockModeType.PESSIMISTIC_WRITE);
        if (product == null) {
            throw new UnknownProductException(productId);
        }
        if (product.stock < quantity) {
            audit.record("REJECTED_STOCK", productId);
            throw new OutOfStockException(productId, quantity, product.stock);
        }
        product.stock -= quantity;

        CustomerOrder order = new CustomerOrder();
        order.productId = productId;
        order.quantity = quantity;
        order.total = product.price.multiply(BigDecimal.valueOf(quantity));
        order.createdAt = Instant.now();
        order.persist();

        if (order.total.compareTo(PAYMENT_LIMIT) > 0) {
            audit.record("REJECTED_PAYMENT", productId);
            throw new PaymentRefusedException(order.total);
        }
        audit.record("PLACED", productId);
        return order;
    }
}
