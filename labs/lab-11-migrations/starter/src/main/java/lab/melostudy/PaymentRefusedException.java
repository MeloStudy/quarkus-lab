package lab.melostudy;

import java.math.BigDecimal;

/**
 * The payment of an order was refused. This is a CHECKED exception (it extends {@link Exception}, not
 * {@link RuntimeException}), which matters: a transaction does NOT roll back for checked exceptions
 * unless you ask for it.
 */
public class PaymentRefusedException extends Exception {

    public PaymentRefusedException(BigDecimal total) {
        super("Payment of " + total + " was refused");
    }
}
