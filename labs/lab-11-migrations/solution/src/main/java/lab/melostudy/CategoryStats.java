package lab.melostudy;

/** One row of the category report: computed by the database, never by loading products into memory. */
public record CategoryStats(String category, long products, long stock, double averagePrice) {
}
