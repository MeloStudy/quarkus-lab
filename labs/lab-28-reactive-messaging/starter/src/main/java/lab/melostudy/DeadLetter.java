package lab.melostudy;

/** An order that could not be processed for a reason that is not its own fault, kept for a human or a replay. */
public record DeadLetter(Order order, String reason) {
}
