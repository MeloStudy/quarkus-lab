package lab.tracker;

import jakarta.validation.constraints.NotNull;

public record TransitionRequest(@NotNull Status to) {
}
