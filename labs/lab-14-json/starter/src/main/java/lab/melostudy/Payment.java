package lab.melostudy;

/**
 * How the guest pays.
 * TODO (task 3): the JSON property "method" ("card" or "transfer") must say which record to build, when reading
 * and when writing. The client may only choose among the types declared here.
 */
public sealed interface Payment {

    record Card(String last4) implements Payment {
    }

    record Transfer(String iban) implements Payment {
    }
}
