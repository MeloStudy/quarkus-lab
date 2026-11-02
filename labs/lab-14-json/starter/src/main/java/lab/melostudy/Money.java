package lab.melostudy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * An amount in a currency.
 * TODO (task 4): on the wire it must be ONE string such as "120.50 EUR", not an object. This class must stay free
 * of Jackson annotations (imagine it lives in a shared library you cannot change).
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public Money times(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }
}
