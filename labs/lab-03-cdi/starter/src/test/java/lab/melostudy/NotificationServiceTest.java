package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class NotificationServiceTest {

    @Inject
    NotificationService service;

    @Test
    void emailUsesTheEmailChannel() {
        assertEquals("email:ana@acme.com:hola", service.emailTo("ana@acme.com", "hola"));
    }

    @Test
    void smsUsesTheSmsChannel() {
        assertEquals("sms:+34600111222:hola", service.smsTo("+34600111222", "hola"));
    }

    @Test
    void broadcastReachesEveryChannel() {
        assertEquals(List.of("email:x:hi", "sms:x:hi"), service.broadcast("x", "hi"));
    }
}
