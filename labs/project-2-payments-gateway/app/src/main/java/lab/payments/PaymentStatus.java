package lab.payments;

/**
 * What we know about the money of one payment.
 * <ul>
 *   <li>{@code PENDING}: the provider is being asked right now. It never outlives the request that created it.</li>
 *   <li>{@code CAPTURED}: the provider charged the card.</li>
 *   <li>{@code DECLINED}: the provider refused the card. Final.</li>
 *   <li>{@code UNKNOWN}: we asked and never learned the answer (timeout, broken connection, garbage). The card may or
 *       may not have been charged. Only a lookup at the provider ({@code reconcile}) can settle it.</li>
 *   <li>{@code FAILED}: the provider refused to even look at the request. Nobody was charged. Final.</li>
 *   <li>{@code REFUNDED}: a captured payment that was given back. Final.</li>
 * </ul>
 */
public enum PaymentStatus {
    PENDING, CAPTURED, DECLINED, UNKNOWN, FAILED, REFUNDED
}
