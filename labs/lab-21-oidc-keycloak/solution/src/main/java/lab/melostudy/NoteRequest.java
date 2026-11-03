package lab.melostudy;

import jakarta.validation.constraints.NotBlank;

public record NoteRequest(@NotBlank String title) {
}
