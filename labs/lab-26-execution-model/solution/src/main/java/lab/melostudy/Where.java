package lab.melostudy;

import io.quarkus.runtime.BlockingOperationControl;
import io.vertx.core.Context;

/**
 * Where the code that built this answer was running. {@code kind} is what the tests look at; {@code thread} and
 * {@code blockingAllowed} are there for you to read.
 */
public record Where(String thread, String kind, boolean blockingAllowed) {

    public static Where here() {
        Thread current = Thread.currentThread();
        String kind;
        if (current.isVirtual()) {
            kind = "VIRTUAL";
        } else if (Context.isOnEventLoopThread()) {
            kind = "EVENT_LOOP";
        } else {
            kind = "WORKER";
        }
        return new Where(current.getName(), kind, BlockingOperationControl.isBlockingAllowed());
    }
}
