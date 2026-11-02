package lab.melostudy;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "app")
public interface AppConfig {

    String name();

    @WithDefault("0.1.0")
    String version();

    List<String> tags();

    String baseUrl();            // app.base-url (kebab-case por defecto)

    Contact contact();

    interface Contact {
        String email();

        Optional<String> phone();
    }
}
