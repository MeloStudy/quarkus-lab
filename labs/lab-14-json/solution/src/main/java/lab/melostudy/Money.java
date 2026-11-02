package lab.melostudy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * An amount in a currency. On the wire it is ONE string such as {@code "120.50 EUR"} (see {@link MoneyJson}),
 * not an object: this class knows nothing about JSON.
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
