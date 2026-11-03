package lab.melostudy;

public record Order(long id, String sku, int quantity, String type, String status) {

    public Order withStatus(String newStatus) {
        return new Order(id, sku, quantity, type, newStatus);
    }
}
