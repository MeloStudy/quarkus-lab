# Lab 02 - Configuración y perfiles

**Duración estimada:** 2 h · **Quarkus:** 3.40.1.1 LTS · **Java:** 21 · **Tipo:** A (starter + solution)

## Por qué importa
Una misma aplicación corre en tu máquina, en CI, en staging y en producción, y cada entorno necesita valores distintos (URLs, credenciales, textos, límites). En un proyecto real casi nunca ejecutas el dev mode: despliegas **un jar o una imagen** y le cambias la configuración desde fuera (variables de entorno, ConfigMaps, secretos). Este lab cubre ese recorrido completo, incluidas las trampas que más tiempo hacen perder en el trabajo: no saber **qué valor gana**, arrancar con configuración inválida y cambiar en runtime propiedades que solo se leen al compilar.

## Conceptos
- `@ConfigProperty`: valor obligatorio, `defaultValue` y `Optional<T>`
- `@ConfigMapping`: configuración tipada (grupos anidados, `List`, `@WithDefault`) y validación con Bean Validation
- Expresiones de propiedades: `${otra.propiedad:valor-por-defecto}`
- Perfiles: `dev`, `test`, `prod` y perfiles propios (`%staging.`)
- Precedencia de fuentes: `-D` > variables de entorno > `.env` > `config/application.properties` > `application.properties` del jar
- Propiedades **build-time** (fijas al compilar) frente a **runtime** (sobrescribibles)
- Sobrescribir configuración en tests con `QuarkusTestProfile`
- Fail-fast: la aplicación no arranca con configuración inválida

## Documentación
- [Configuración: referencia](https://quarkus.io/guides/config-reference) (precedencia, perfiles, variables de entorno, expresiones)
- [Mapear la configuración a objetos](https://quarkus.io/guides/config-mappings) (incluye la sección de Bean Validation)
- [Testing en Quarkus](https://quarkus.io/guides/getting-started-testing) (perfiles de test)

## Escenario
`starter/` tiene dos endpoints que hoy devuelven valores fijos:
- `GET /greeting` devuelve un saludo.
- `GET /info` devuelve datos de la aplicación (hoy responde `TODO`) a partir de un `AppConfig` que solo tiene `name()`.

Hay 11 tests; al empezar, 10 fallan. El que comprueba los valores por defecto del saludo pasa desde el inicio y **debe seguir pasando** (es una guarda de regresión).

## Contrato de configuración

| Propiedad | Valor / regla |
|---|---|
| `greeting.message` | `Hola` (ya está en el archivo) |
| `greeting.name` | **No** la definas; es opcional. Si falta, el saludo usa `mundo` |
| `greeting.punctuation` | **No** la definas; valor por defecto `!` en el código |
| `app.name` | `config-lab` (ya está en el archivo) |
| `app.version` | **No** la definas; valor por defecto `0.1.0` en el mapping |
| `app.tags` | `quarkus,config,lab` (lista) |
| `app.contact.email` | `dev@lab.melostudy` (grupo anidado, obligatorio y con formato de correo) |
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
2. [ ] **Tarea 2 - `@ConfigMapping`:** completa `AppConfig` (versión con valor por defecto, lista de tags, `baseUrl` y un grupo anidado de contacto con `email` y un `phone` opcional), inyéctalo en `InfoResource` y devuelve el formato indicado. Agrega las propiedades al archivo según el contrato.
3. [ ] **Tarea 3 - Expresiones:** define `app.base-url` usando una expresión que tome `api.host` y, si no existe, `localhost`.
4. [ ] **Tarea 4 - Perfil propio:** agrega las propiedades `%staging.` del contrato. Luego arranca en dev mode con ese perfil y compruébalo:
   - PowerShell: `.\mvnw.cmd quarkus:dev "-Dquarkus.profile=staging"`
   - Git Bash: `mvn quarkus:dev -Dquarkus.profile=staging`
5. [ ] **Tarea 5 - Fail-fast:** agrega la extensión `hibernate-validator` y exige que `contact.email` no esté vacío y tenga formato de correo. Cuando los tests pasen, prueba a mano: pon `app.contact.email=nope` en `application.properties`, arranca en dev mode y lee el error (`ConfigValidationException: Configuration validation failed`). Después restaura el valor. Reflexiona: ¿qué pasaría en un despliegue real si la aplicación **sí** arrancara con ese valor?
6. [ ] **Tarea 6 - Ejecutar el jar y precedencia (sin tests):** así corre en producción. Empaqueta y ejecuta (sin dev mode, perfil `prod`):
   - PowerShell: `.\mvnw.cmd package -DskipTests` y luego `java -jar target\quarkus-app\quarkus-run.jar`
   - Detén la app con `Ctrl+C` entre pruebas.

   **Anota qué responde `/greeting` antes de ejecutar** cada caso (¿lo predijiste?):

   | # | Situación |
   |---|---|
   | a | Solo el `application.properties` empaquetado |
   | b | Variable de entorno `GREETING_MESSAGE=Env` |
   | c | La variable de entorno anterior **y** `-Dgreeting.message=Sys` |
   | d | Un archivo `config/application.properties` (carpeta `config` junto a donde ejecutas) con `greeting.message=ConfigDir` |
   | e | El caso d **y** la variable de entorno |
   | f | Agregas `%prod.greeting.message=Prod` en `application.properties` y vuelves a empaquetar |
   | g | El caso f **y** el `config/application.properties` del caso d |
   | h | El caso g **y** la variable de entorno |

   Cómo pasar valores:
   - Propiedad de sistema (PowerShell, **entre comillas**): `java "-Dgreeting.message=Sys" -jar target\quarkus-app\quarkus-run.jar`
   - Variable de entorno (PowerShell): `$env:GREETING_MESSAGE='Env'; java -jar target\quarkus-app\quarkus-run.jar` (luego `Remove-Item Env:GREETING_MESSAGE`)
   - Git Bash: `GREETING_MESSAGE=Env java -jar target/quarkus-app/quarkus-run.jar`
7. [ ] **Tarea 7 - Build-time vs runtime:** con el jar ya empaquetado, arráncalo cambiando `quarkus.http.root-path`:
   - PowerShell: `java "-Dquarkus.http.root-path=/x" -jar target\quarkus-app\quarkus-run.jar`
   - Git Bash: antepón `MSYS_NO_PATHCONV=1` (si no, convierte `/x` en una ruta de Windows).

   Prueba `/greeting` y `/x/greeting`, y lee el log. ¿Qué dice Quarkus y por qué? Compara con `quarkus.http.port`, que sí se puede cambiar al arrancar. En la referencia de configuración, busca cómo se marcan las propiedades fijadas en build-time.

> No comitees un archivo `.env`: es el sitio típico donde acaban los secretos locales (ya está en el `.gitignore`).

**Criterio de éxito:** los 11 tests pasan (`mvn test` en `starter/`, o `.\mvnw.cmd verify` en PowerShell) y has completado a mano las tareas 4 a 7.

## En proyectos reales
- **Los secretos nunca van en el repositorio** (contraseñas, tokens, claves). En `application.properties` solo valores por defecto seguros para desarrollo; en producción llegan por variable de entorno o secreto del orquestador.
- **Configuración fuera del artefacto:** el mismo jar/imagen se promociona entre entornos y solo cambia la configuración (principio de las *12-factor apps*). En Kubernetes, un ConfigMap montado como fichero suele terminar siendo el `config/application.properties` de la tarea 6, y un Secret suele inyectarse como variable de entorno.
- **Usa `%prod.` con cautela:** sirve para defaults no sensibles; no metas ahí URLs ni credenciales reales. Como viste, cualquier fuente externa de mayor prioridad lo sobrescribe, y eso es lo que quieres.
- **Prefiere `@ConfigMapping` a `@ConfigProperty` suelto:** agrupa, tipa y valida en un único sitio, y se testea sin arrancar Quarkus (como en la tarea 5).
- **Fail-fast:** una aplicación que arranca mal configurada falla en producción a las 3 a. m.; una que se niega a arrancar falla en el despliegue, donde el orquestador detiene el rollout y la versión anterior sigue sirviendo.
- **Build-time vs runtime:** antes de prometer "lo cambiamos por variable de entorno en producción", comprueba que esa propiedad no sea de build-time.

## Reto extra
1. Extrae `greeting.*` a un segundo `@ConfigMapping` (`GreetingConfig`) y elimina los `@ConfigProperty`. ¿Qué ganas y qué pierdes?
2. Muestra `contact.phone` en `/info` solo si existe, sin romper los tests.
3. Abre el Dev UI en dev mode, localiza el editor de configuración y cambia un valor en caliente.

## Pistas
<details><summary>Pista 1 (dirección)</summary>

- Tarea 1: tres campos con `@ConfigProperty`; uno de ellos es de un tipo especial para valores que pueden no existir.
- Tarea 2: en la guía de *config mappings* busca grupos anidados y valores por defecto. Los nombres de los métodos se convierten a `kebab-case`.
- Tarea 3: la sintaxis de expresión con valor por defecto está en la sección *Property expressions* de la referencia.
- Tarea 5: las anotaciones de validación se ponen sobre los métodos del mapping.
</details>

<details><summary>Pista 2 (más concreta)</summary>

- `@ConfigProperty(name = "...", defaultValue = "!")` y `Optional<String>`.
- `@WithDefault("0.1.0")`; un método que devuelve otra interfaz crea el grupo anidado; `List<String>` lee valores separados por comas.
- Un perfil se activa con el prefijo `%nombre.` en la propiedad.
- Extensión: `quarkus extension add hibernate-validator` o `mvn quarkus:add-extension -Dextensions=hibernate-validator`. Anotaciones: `@NotBlank` y `@Email` (paquete `jakarta.validation.constraints`).
</details>

<details><summary>Pista 3 - si el arranque falla con <code>SRCFG00050 ... does not map to any root</code></summary>

Todo lo que cuelga del prefijo de un `@ConfigMapping` debe estar mapeado en la interfaz; si no, Quarkus se niega a arrancar. Por eso la variable de la expresión de la Tarea 3 se llama `api.host` y **no** `app.host`: está fuera del prefijo `app`. Te puede pasar también si añades `app.tags` al archivo antes de declarar `tags()` en `AppConfig`.
</details>

<details><summary>Respuestas de la tarea 6 (míralas después de predecir)</summary>

| # | Resultado de `/greeting` | Por qué |
|---|---|---|
| a | `Hola, mundo!` | Solo hay un valor |
| b | `Env, mundo!` | Variables de entorno (300) > `application.properties` (250) |
| c | `Sys, mundo!` | `-D` (400) > variables de entorno (300) |
| d | `ConfigDir, mundo!` | `config/application.properties` (260) > el del jar (250) |
| e | `Env, mundo!` | Variable de entorno (300) > `config/` (260) |
| f | `Prod, mundo!` | En el mismo archivo, `%prod.` pisa a la propiedad sin perfil |
| g | `ConfigDir, mundo!` | **Sorpresa:** gana la fuente con más prioridad aunque su propiedad no tenga perfil; el perfil solo desempata dentro de una misma fuente |
| h | `Env, mundo!` | Variable de entorno (300) por encima de todo lo anterior |

(Verificado ejecutando cada caso sobre el jar con Quarkus 3.40.1.1. En dev mode se cumple la misma regla: una variable de entorno gana incluso a una propiedad `%dev.` del archivo.)
</details>

<details><summary>Respuesta de la tarea 7</summary>

`quarkus.http.root-path` se fija al compilar: Quarkus lo ignora en runtime y avisa con `Build time property cannot be changed at runtime: quarkus.http.root-path is set to '/x' but it is build time fixed to '/'`. Las rutas siguen siendo `/greeting` y `/x/greeting` responde 404. Quarkus optimiza en build-time (elige extensiones, genera código, reduce el arranque), así que no todo se puede cambiar después. `quarkus.http.port`, en cambio, es de runtime.
</details>

## Autoevaluación
Responde sin mirar el código; deberías poder explicarlo en una entrevista.

1. ¿Por qué la variable de una expresión no puede llamarse `app.host` si existe un `@ConfigMapping(prefix = "app")`?
2. En Kubernetes tienes un ConfigMap montado como `config/application.properties` y una variable de entorno con la misma clave. ¿Cuál gana y por qué?
3. Un compañero dice "cambiaremos el context path en producción con una variable de entorno". ¿Qué le respondes?
4. ¿Dónde guardarías la contraseña de la base de datos de producción? ¿Y la URL de la base de datos de desarrollo?
5. ¿Qué ventaja tiene que la aplicación falle al arrancar frente a fallar en la primera petición?

La solución completa está en `solution/`.

## Retrospectiva
- ¿Qué caso de precedencia no predije bien y por qué?
- ¿Cuándo usaría `@ConfigProperty` y cuándo `@ConfigMapping`?
- ¿Qué parte del recorrido (dev mode, tests, jar) me resultó más cercana a lo que haría en el trabajo?
