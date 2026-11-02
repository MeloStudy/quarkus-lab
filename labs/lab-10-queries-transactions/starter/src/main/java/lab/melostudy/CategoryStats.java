package lab.melostudy;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** One row of the category report: computed by the database, never by loading products into memory. */
@Schema(description = "Aggregated figures of one category")
public record CategoryStats(String category, long products, long stock, double averagePrice) {
}
