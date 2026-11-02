package lab.melostudy;

public interface Notifier {

    /** Envía el mensaje y devuelve un acuse con el formato {@code canal:destino:mensaje}. */
    String send(String to, String message);
}
