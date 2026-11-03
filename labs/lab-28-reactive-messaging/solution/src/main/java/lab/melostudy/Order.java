package lab.melostudy;

/** An order that travels through the channels. The id is assigned when the order is accepted at the door. */
public record Order(String id, String sku, int quantity) {
}
