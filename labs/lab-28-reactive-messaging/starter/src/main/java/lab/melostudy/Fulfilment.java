package lab.melostudy;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The last stage: reserve the stock. This is a first version that works when nothing goes wrong. Read it, then change it
 * in the tasks.
 */
@ApplicationScoped
public class Fulfilment {

    @Inject
    Warehouse warehouse;

    @Inject
    Ledger ledger;

    // TODO task 2: what happens to the sender, and to the channel, when reserve() throws? Run the failure tests and read the
    //   log before changing anything. An order that is out of stock must reach its sender as that failure (409), must be
    //   OUT_OF_STOCK in the ledger, and must not stop the orders behind it.
    // TODO task 3: an order that failed because the warehouse is unavailable (not out of stock) must be sent to the
    //   "dead-letters" channel (an Emitter<DeadLetter>) before the failure goes on; the ledger says DEAD. Out of stock and
    //   refused orders do not go to the dead letters.
    // TODO task 4: this call blocks on a slow driver. Where does it run?
    @Incoming("fulfilment")
    public CompletionStage<Void> fulfil(Message<Order> message) {
        Order order = message.getPayload();
        warehouse.reserve(order);
        ledger.record(order, OrderStatus.FULFILLED);
        return message.ack();
    }
}
