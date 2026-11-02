package lab.melostudy;

import java.time.Instant;

public record Order(long id, String item, int quantity, Instant createdAt) {
}
