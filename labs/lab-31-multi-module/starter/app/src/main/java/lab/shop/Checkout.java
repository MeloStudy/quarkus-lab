package lab.shop;

import jakarta.enterprise.context.ApplicationScoped;
import lab.pricing.Logged;

/** A bean of the application that uses an interceptor binding which lives in the library. */
@ApplicationScoped
public class Checkout {

    @Logged
    public String checkout(String customer) {
        return "checked out " + customer;
    }
}
