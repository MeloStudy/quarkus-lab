package lab.melostudy;

/** Number of reviews of a product and their average rating (0 when there are none). */
public record ReviewSummary(long count, double average) {
}
