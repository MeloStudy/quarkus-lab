# Lab 02 - Configuración y perfiles

**Duración estimada:** 1,5 h · **Quarkus:** 3.40.1.1 LTS · **Java:** 21 · **Tipo:** A (starter + solution)

## Por qué importa
Una misma aplicación corre en tu máquina, en CI, en staging y en producción, y cada entorno necesita valores distintos (URLs, credenciales, textos, límites). Quarkus resuelve esto con una configuración por capas: valores por defecto en el archivo, perfiles por entorno y sobrescritura externa sin recompilar. Entender **quién gana cuando hay varios valores** te ahorra horas de depuración.

## Conceptos
- `@ConfigProperty`: valor obligatorio, `defaultValue` y `Optional<T>`
- `@ConfigMapping`: agrupar propiedades en una interfaz tipada (grupos anidados, `List`, `@WithDefault`)
- Expresiones de propiedades: `${otra.propiedad:valor-por-defecto}`
- Perfiles: `dev`, `test`, `prod` y perfiles propios (`%staging.`)
- Orden de precedencia: `-D` > variables de entorno > `.env` > `application.properties`
- Sobrescribir configuración en tests con `QuarkusTestProfile`

## Documentación
- [Configuración: referencia](https://quarkus.io/guides/config-reference) (precedencia, perfiles, variables de entorno, expresiones)
- [Mapear la configuración a objetos](https://quarkus.io/guides/config-mappings)
- [Testing en Quarkus](https://quarkus.io/guides/getting-started-testing) (perfiles de test)

## Escenario
`starter/` tiene dos endpoints que hoy devuelven valores fijos:
- `GET /greeting` devuelve un saludo.
- `GET /info` devuelve datos de la aplicación (hoy responde `TODO`).

Tu trabajo es que ambos lean su contenido de la configuración y que cambien según el perfil. Hay 8 tests; al empezar, 7 fallan (el que comprueba los valores por defecto del saludo pasa desde el inicio y **debe seguir pasando**).

## Contrato de configuración

| Propiedad | Valor / regla |
|---|---|
| `greeting.message` | `Hola` (ya está en el archivo) |
| `greeting.name` | **No** la definas; es opcional. Si falta, el saludo usa `mundo` |
| `greeting.punctuation` | **No** la definas; valor por defecto `!` en el código |
| `app.name` | `config-lab` |
| `app.version` | **No** la definas; valor por defecto `0.1.0` en el mapping |
| `app.tags` | `quarkus,config,lab` (lista) |
| `app.contact.email` | `dev@lab.melostudy` (grupo anidado) |
| `app.base-url` | `https://<api.host>/api`, con `api.host` valiendo `localhost` si no existe |
| `%staging.greeting.message` | `Hola desde staging` |
| `%staging.app.name` | `config-lab-staging` |

Formato de `GET /greeting`: `<message>, <name o mundo><punctuation>` → por defecto `Hola, mundo!`

Formato de `GET /info`:
```
config-lab v0.1.0 | tags=quarkus,config,lab | contact=dev@lab.melostudy | url=https://localhost/api
```

## Tareas
1. [ ] **Tarea 1 - `@ConfigProperty`:** haz que `GreetingResource` lea `greeting.message` (obligatoria), `greeting.name` (opcional) y `greeting.punctuation` (con valor por defecto).
2. [ ] **Tarea 2 - `@ConfigMapping`:** crea una interfaz con prefijo `app` (nombre, versión con valor por defecto, lista de tags y un grupo anidado de contacto), inyéctala en `InfoResource` y devuelve el formato indicado. Agrega las propiedades al archivo según el contrato.
3. [ ] **Tarea 3 - Expresiones:** define `app.base-url` usando una expresión que tome `api.host` y, si no existe, `localhost`.
4. [ ] **Tarea 4 - Perfil propio:** agrega las propiedades `%staging.` del contrato. Luego arranca en dev mode con ese perfil y compruébalo:
   - PowerShell: `.\mvnw.cmd quarkus:dev "-Dquarkus.profile=staging"`
   - Git Bash: `mvn quarkus:dev -Dquarkus.profile=staging`
5. [ ] **Exploración de precedencia (sin tests):** con la app en dev mode, prueba cada caso y **anota qué responde `/greeting` antes de ejecutar** (¿lo predijiste?):

   | # | Situación |
   |---|---|
   | a | Solo `application.properties` |
   | b | Variable de entorno `GREETING_MESSAGE=Env` |
   | c | La variable de entorno anterior **y** `-Dgreeting.message=Sys` |
   | d | Un archivo `.env` en la carpeta del proyecto con `GREETING_MESSAGE=DotEnv` |
   | e | La variable de entorno **y** el `.env` a la vez |
   | f | `%dev.greeting.message=Dev` en `application.properties` |
   | g | El caso f **y** la variable de entorno `GREETING_MESSAGE=Env` |

   Cómo definir una variable de entorno solo para un comando:
   - PowerShell: `$env:GREETING_MESSAGE='Env'; .\mvnw.cmd quarkus:dev`
   - Git Bash: `GREETING_MESSAGE=Env mvn quarkus:dev`

   > No comitees el archivo `.env`: es el sitio típico donde acaban los secretos locales.

**Criterio de éxito:** los 8 tests pasan (`mvn test` en `starter/`, o `.\mvnw.cmd verify` en PowerShell).

## Reto extra
1. Agrega la extensión `hibernate-validator` y anota el `@ConfigMapping` para exigir que `app.contact.email` tenga forma de correo (`@Email`). Pon un valor inválido y mira el mensaje con el que **falla el arranque**. ¿Por qué es mejor fallar al inicio que en la primera petición?
2. Agrega `app.contact.phone` (el mapping ya contempla `Optional<String>`) y muéstralo en `/info` solo si existe.

## Pistas
<details><summary>Pista 1 (dirección)</summary>

- Para la Tarea 1 necesitas tres campos con `@ConfigProperty`; uno de ellos es de un tipo especial para valores que pueden no existir.
- Para la Tarea 2 busca en la guía de *config mappings* cómo declarar grupos anidados y valores por defecto. Los nombres de los métodos se convierten a `kebab-case`.
- Para la Tarea 3, la sintaxis de expresión con valor por defecto está en la sección *Property expressions* de la referencia.
</details>

<details><summary>Pista 2 (más concreta)</summary>

- `@ConfigProperty(name = "...", defaultValue = "!")` y `Optional<String>`.
- `@ConfigMapping(prefix = "app")` sobre una **interfaz**; `@WithDefault("0.1.0")`; un método que devuelve otra interfaz crea el grupo anidado; `List<String>` lee valores separados por comas.
- Un perfil se activa con el prefijo `%nombre.` en la propiedad.
</details>

<details><summary>Pista 3 - si el arranque falla con <code>SRCFG00050 ... does not map to any root</code></summary>

Todo lo que cuelga del prefijo de un `@ConfigMapping` debe estar mapeado en la interfaz; si no, Quarkus se niega a arrancar. Por eso la variable de la expresión de la Tarea 3 se llama `api.host` y **no** `app.host`: está fuera del prefijo `app`.
</details>

<details><summary>Respuestas de la exploración (míralas después de predecir)</summary>

| # | Resultado de `/greeting` | Por qué |
|---|---|---|
| a | `Hola, mundo!` | Solo hay un valor |
| b | `Env, mundo!` | Variables de entorno (300) > `application.properties` (250) |
| c | `Sys, mundo!` | `-D` (400) > variables de entorno (300) |
| d | `DotEnv, mundo!` | `.env` (295) > `application.properties` (250) |
| e | `Env, mundo!` | Variable de entorno (300) > `.env` (295) |
| f | `Dev, mundo!` | En dev mode `%dev.` pisa a la propiedad sin perfil, en el mismo archivo |
| g | `Env, mundo!` | **Sorpresa:** la fuente con más prioridad gana aunque su propiedad no tenga perfil; el perfil solo desempata dentro de una misma fuente |

(Verificado ejecutando cada caso en dev mode con Quarkus 3.40.1.1.)
</details>

La solución completa está en `solution/`.

## Retrospectiva
- ¿Qué caso de precedencia no predije bien y por qué?
- ¿Cuándo usaría `@ConfigProperty` y cuándo `@ConfigMapping`?
- ¿Qué configuración **nunca** debería ir en `application.properties` dentro del repositorio?
