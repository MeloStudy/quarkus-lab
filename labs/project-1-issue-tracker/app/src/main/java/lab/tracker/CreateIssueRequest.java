package lab.tracker;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What a client may send to open an issue. Status, number, version and dates are NOT here: the client cannot
 * choose them, so the contract does not even have a field for them (and unknown fields are rejected).
 */
public record CreateIssueRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2000) String description,
        @NotNull IssueType type,
        Priority priority,
        @Size(max = 5) Set<@NotNull @Pattern(regexp = "[a-z0-9-]{1,20}", message = "must be 1 to 20 lowercase letters, digits or dashes") String> labels) {
}
