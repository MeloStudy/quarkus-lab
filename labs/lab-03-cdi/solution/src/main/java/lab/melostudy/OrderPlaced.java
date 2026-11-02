package lab.melostudy;

/** Evento de dominio: se dispara cuando se registra un pedido. */
public record OrderPlaced(Order order) {
}
