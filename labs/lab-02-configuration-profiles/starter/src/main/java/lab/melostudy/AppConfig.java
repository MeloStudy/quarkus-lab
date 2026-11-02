package lab.melostudy;

import io.smallrye.config.ConfigMapping;

/**
 * Typed application configuration (prefix "app").
 *
 * TODO (Task 2): complete the mapping following the README contract
 *   version  -> with a default value
 *   tags     -> list of strings
 *   baseUrl  -> property app.base-url
 *   contact  -> nested group with email (and optional phone)
 *
 * TODO (Task 5): add the hibernate-validator extension and validate the email.
 */
@ConfigMapping(prefix = "app")
public interface AppConfig {

    String name();
}
