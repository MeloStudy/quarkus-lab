package lab.tracker;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PUT replaces the editable part of an issue: what is absent is cleared (description, assignee, labels).
 * Status is not editable here, it only moves through a transition.
 */
public record UpdateIssueRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2000) String description,
        @NotNull Priority priority,
        @Size(min = 1, max = 40) String assignee,
        @Size(max = 5) Set<@NotNull @Pattern(regexp = "[a-z0-9-]{1,20}", message = "must be 1 to 20 lowercase letters, digits or dashes") String> labels) {
}
