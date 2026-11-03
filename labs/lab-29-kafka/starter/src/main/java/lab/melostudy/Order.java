package lab.melostudy;

/** The event that travels through Kafka as JSON. The key of the Kafka record is the customer; the id identifies the order. */
public record Order(String id, String customer, String sku, int quantity, String note) {
}
