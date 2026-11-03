package lab.melostudy;

/** What the client sends. A missing quantity is 0, and the intake step refuses it. */
public record OrderRequest(String sku, int quantity) {
}
