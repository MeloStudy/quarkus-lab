package lab.melostudy;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "app")
public interface AppConfig {

    String name();

    @WithDefault("0.1.0")
    String version();

    List<String> tags();

    String baseUrl();            // app.base-url (kebab-case by default)

    Contact contact();

    interface Contact {
        @NotBlank
        @Email
        String email();

        Optional<String> phone();
    }
}
