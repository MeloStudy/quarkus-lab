package lab.melostudy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What the API accepts when adding a review. The {@code type} property picks the concrete record. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReviewInput.Customer.class, name = "CUSTOMER"),
        @JsonSubTypes.Type(value = ReviewInput.Critic.class, name = "CRITIC")
})
public sealed interface ReviewInput {

    int rating();

    String comment();

    record Customer(
            @Min(1) @Max(5) int rating,
            @NotBlank @Size(max = 500) String comment,
            Boolean verifiedPurchase) implements ReviewInput {
    }

    record Critic(
            @Min(1) @Max(5) int rating,
            @NotBlank @Size(max = 500) String comment,
            @NotBlank @Size(max = 100) String outlet) implements ReviewInput {
    }
}
