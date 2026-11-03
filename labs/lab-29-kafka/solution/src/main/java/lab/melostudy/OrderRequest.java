package lab.melostudy;

/** What a client posts. The id is assigned by the service. */
public record OrderRequest(String customer, String sku, int quantity, String note) {
}
