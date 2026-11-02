package lab.melostudy;

import java.math.BigDecimal;

/** What the remote exchange-rate service returns: how many units of {@code currency} one USD buys. */
public record Rate(String currency, BigDecimal rate) {
}
