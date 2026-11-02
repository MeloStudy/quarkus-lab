package lab.melostudy;

import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.ConfigValidator;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tarea 5: una configuración inválida debe impedir el arranque (fail-fast).
 * Es un test unitario puro: construye la configuración a mano, sin levantar Quarkus.
 */
class AppConfigValidationTest {

    /** Validador de Bean Validation para @ConfigMapping; solo existe si se agregó quarkus-hibernate-validator. */
    private static ConfigValidator beanValidator() {
        try {
            return (ConfigValidator) Class.forName("io.smallrye.config.validator.BeanValidationConfigValidatorImpl")
                .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            return fail("Falta la extensión quarkus-hibernate-validator (Tarea 5): " + e);
        }
    }

    private static SmallRyeConfig buildWithEmail(String email) {
        return new SmallRyeConfigBuilder()
            .withValidator(beanValidator())
            .withValidateUnknown(false)
            .withMapping(AppConfig.class)
            .withSources(new PropertiesConfigSource(Map.of(
                "app.name", "config-lab",
                "app.tags", "quarkus",
                "app.base-url", "https://localhost/api",
                "app.contact.email", email), "test", 100))
            .build();
    }

    @Test
    void validEmailIsAccepted() {
        assertDoesNotThrow(() -> buildWithEmail("dev@lab.melostudy"));
    }

    @Test
    void invalidEmailFailsFast() {
        assertThrows(ConfigValidationException.class, () -> buildWithEmail("no-es-un-correo"));
    }

    @Test
    void blankEmailFailsFast() {
        assertThrows(ConfigValidationException.class, () -> buildWithEmail(" "));
    }
}
