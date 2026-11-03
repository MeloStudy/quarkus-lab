package lab.melostudy;

/** {@code source} is {@code carrier} for a fresh price and {@code cache} for the last price we knew. */
public record Quote(String city, double price, String source) {
}
