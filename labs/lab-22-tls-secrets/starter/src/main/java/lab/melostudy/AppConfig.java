package lab.melostudy;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

/** Settings of the application. {@code api-key} is a secret: it must never be written in the repository. */
@ConfigMapping(prefix = "app")
public interface AppConfig {

    String name();

    @WithName("api-key")
    String apiKey();
}
