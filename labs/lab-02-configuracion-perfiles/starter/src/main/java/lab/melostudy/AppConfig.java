package lab.melostudy;

import io.smallrye.config.ConfigMapping;

/**
 * Configuración tipada de la aplicación (prefijo "app").
 *
 * TODO (Tarea 2): completar el mapping según el contrato del README
 *   version  -> con valor por defecto
 *   tags     -> lista de textos
 *   baseUrl  -> propiedad app.base-url
 *   contact  -> grupo anidado con email (y phone opcional)
 *
 * TODO (Tarea 5): agregar la extensión hibernate-validator y validar el email.
 */
@ConfigMapping(prefix = "app")
public interface AppConfig {

    String name();
}
