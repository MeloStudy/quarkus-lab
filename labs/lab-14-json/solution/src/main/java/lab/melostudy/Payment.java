package lab.melostudy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * How the guest pays. The JSON property {@code method} says which record to build. The names are an explicit
 * list ({@code Id.NAME} + {@code @JsonSubTypes}): the client can only pick among the types we declare.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "method")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Payment.Card.class, name = "card"),
        @JsonSubTypes.Type(value = Payment.Transfer.class, name = "transfer")
})
public sealed interface Payment {

    record Card(String last4) implements Payment {
    }

    record Transfer(String iban) implements Payment {
    }
}
