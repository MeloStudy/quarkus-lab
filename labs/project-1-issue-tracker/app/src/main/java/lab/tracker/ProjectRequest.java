package lab.tracker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{2,5}", message = "must be 2 to 5 capital letters") String code,
        @NotBlank @Size(max = 80) String name) {
}
