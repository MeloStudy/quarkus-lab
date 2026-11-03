package lab.melostudy;

/** What the ledger remembers about an order that got past the intake step. */
public enum OrderStatus {
    /** Stock reserved. */
    FULFILLED,
    /** The warehouse has none of that sku. Retrying would not help. */
    OUT_OF_STOCK,
    /** The warehouse kept failing: the order was sent to the dead letters. */
    DEAD
}
