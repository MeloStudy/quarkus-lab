package lab.melostudy;

public interface Notifier {

    /** Sends the message and returns an acknowledgement formatted as {@code channel:to:message}. */
    String send(String to, String message);
}
