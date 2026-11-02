package lab.melostudy;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A review as the API shows it. The JSON {@code type} property tells the kinds apart. The Jackson annotations
 * live HERE, on the DTO: the entity hierarchy ({@link Review}) knows nothing about JSON.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReviewResponse.Customer.class, name = "CUSTOMER"),
        @JsonSubTypes.Type(value = ReviewResponse.Critic.class, name = "CRITIC")
})
public sealed interface ReviewResponse {

    record Customer(Long id, int rating, String comment, Instant createdAt, Boolean verifiedPurchase)
            implements ReviewResponse {
    }

    record Critic(Long id, int rating, String comment, Instant createdAt, String outlet) implements ReviewResponse {
    }
}
