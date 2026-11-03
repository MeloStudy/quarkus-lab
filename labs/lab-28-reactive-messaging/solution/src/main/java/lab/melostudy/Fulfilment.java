package lab.melostudy;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lab.melostudy.Exceptions.OutOfStockException;
import lab.melostudy.Exceptions.WarehouseUnavailableException;

/**
 * The last stage: reserve the stock. Three things decide how it is written:
 * <ul>
 * <li>the method takes the <b>payload</b>, so the default acknowledgement is "after the method returns": a return is an ack
 * and an exception is a nack that reaches whoever sent the order, and the channel keeps going;</li>
 * <li>the warehouse driver blocks, so the method is {@code @Blocking}: it runs on a worker, one order at a time and in order;</li>
 * <li>an order that failed for a reason that is not its own fault is not lost: it goes to the {@code dead-letters} channel
 * before the failure is rethrown.</li>
 * </ul>
 */
@ApplicationScoped
public class Fulfilment {

    private final Warehouse warehouse;
    private final Ledger ledger;
    private final Emitter<DeadLetter> deadLetters;

    @Inject
    public Fulfilment(Warehouse warehouse, Ledger ledger, @Channel("dead-letters") Emitter<DeadLetter> deadLetters) {
        this.warehouse = warehouse;
        this.ledger = ledger;
        this.deadLetters = deadLetters;
    }

    @Incoming("fulfilment")
    @Blocking
    public void fulfil(Order order) {
        try {
            warehouse.reserve(order);
            ledger.record(order, OrderStatus.FULFILLED);
        } catch (OutOfStockException e) {
            ledger.record(order, OrderStatus.OUT_OF_STOCK);
            throw e;
        } catch (WarehouseUnavailableException e) {
            ledger.record(order, OrderStatus.DEAD);
            deadLetters.send(new DeadLetter(order, e.getMessage()));
            throw e;
        }
    }
}
