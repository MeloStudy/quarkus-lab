# Lab 01 - Proyecto y dev mode

**Duración estimada:** 1,5 h · **Quarkus:** 3.40.1.1 LTS · **Java:** 21 · **Tipo:** A (starter + solution)

## Por qué importa
El ciclo de trabajo diario en Quarkus gira en torno al **dev mode**: editas, guardas, refrescas y ves el cambio sin reiniciar nada. Dominarlo, junto con el **Dev UI** y la gestión de extensiones, es la base de todo lo demás.

## Conceptos
- Estructura de un proyecto Quarkus (`pom.xml`, BOM de la plataforma, `src/main`, `src/test`, `application.properties`)
- Dev mode y live reload (`quarkus:dev`)
- Continuous testing (tecla `r` en la consola de dev mode)
- Dev UI (`http://localhost:8080/q/dev-ui/`)
- Extensiones: cómo se agregan y qué cambian en el `pom.xml`
- `@QuarkusTest` + RestAssured

## Documentación
- [Getting Started](https://quarkus.io/guides/getting-started)
- [SmallRye Health](https://quarkus.io/guides/smallrye-health)
- [Quarkus CLI](https://quarkus.io/guides/cli-tooling)
- [Dev UI](https://quarkus.io/guides/dev-ui)

> Nota: las guías públicas muestran la última versión (hoy 3.40). Tu proyecto usa 3.40.1.1 (LTS), así que coincide con las guías.

## Escenario
`starter/` es un proyecto recién generado (extensión `rest`) con un solo endpoint `GET /hello`. Hay tres tests que fallan; tu trabajo es dejarlos en verde **trabajando en dev mode**, no en un ciclo de compilar-ejecutar.

## Tareas
1. [ ] Levanta el proyecto en dev mode desde `starter/`:
   - PowerShell: `.\mvnw.cmd quarkus:dev` (o `quarkus dev` si tienes la CLI)
   - Con Git Bash, `./mvnw` puede fallar por `JAVA_HOME`; usa PowerShell o `mvn quarkus:dev`.
2. [ ] Abre `http://localhost:8080/hello` y el Dev UI (`http://localhost:8080/q/dev-ui/`). Responde con lo que veas allí:
   - ¿Qué extensiones están instaladas?
   - ¿Dónde se editan las propiedades de configuración?
   - ¿Dónde ves los endpoints REST expuestos?
3. [ ] **Tarea 1:** con la app corriendo, cambia el texto de `GET /hello` a `Hola desde Quarkus`. Refresca el navegador sin reiniciar.
4. [ ] **Tarea 2:** crea el endpoint `GET /hello/{name}` que responda `Hola, {name}!`.
5. [ ] **Tarea 3:** agrega la extensión `smallrye-health` **sin editar el `pom.xml` a mano**. Comprueba que `/q/health` responde `UP`.
6. [ ] En la consola de dev mode pulsa `r` para ejecutar los tests de forma continua y observa cómo pasan a verde.
7. [ ] **Ejecutar fuera del dev mode:** detén el dev mode, empaqueta y ejecuta el jar, que es lo que se despliega de verdad:
   - PowerShell: `.\mvnw.cmd package -DskipTests` y luego `java -jar target\quarkus-app\quarkus-run.jar`
   - **Antes de probar, predice:** ¿qué perfil se activa?, ¿responden `/hello`, `/q/health` y `/q/dev-ui/`?
   - Mira qué contiene `target\quarkus-app\`: ¿qué tendrías que copiar a un servidor para desplegar?

**Criterio de éxito:** los 3 tests de `GreetingResourceTest` pasan (`mvn test` o `.\mvnw.cmd verify` en `starter/`) y has ejecutado el jar de la tarea 7.

## Qué deberías ver
- Al arrancar, la consola indica `Profile dev activated. Live Coding activated.` y la lista de `Installed features`.
- Tras la Tarea 3, `smallrye-health` aparece en `Installed features` sin haber reiniciado la app.
- `GET /q/health` devuelve algo como `{"status":"UP","checks":[]}`.

## En proyectos reales
- **El dev mode es solo para desarrollar:** nunca se despliega. En el jar el perfil es `prod`, y el Dev UI (`/q/dev-ui/`) no existe (responde 404), pero `/q/health` sí.
- **Se despliega la carpeta `target/quarkus-app/` completa** (`quarkus-run.jar` más `lib/`, `app/` y `quarkus/`), no solo el `.jar` ejecutable. Normalmente la copia una imagen de contenedor (lab 25).
- **Los endpoints de health son los probes del orquestador:** en Kubernetes, `/q/health/live` y `/q/health/ready` suelen ser los *liveness* y *readiness probes*.
- **No fijes versiones de extensiones a mano:** el `pom.xml` importa un BOM de la plataforma que las gestiona. Actualizar Quarkus es cambiar una sola versión (`quarkus.platform.version`).
- **Usa el wrapper (`mvnw`)** para que todo el equipo y CI compilen con la misma versión de Maven.
- **Continuous testing** (`r`) sustituye el ciclo "cambio, compilo, ejecuto tests": úsalo siempre que trabajes con tests.

## Reto extra
Crea un `HealthCheck` propio de tipo liveness que reporte `UP` con un dato adicional (`withData`) y localízalo en `/q/health/live`. Luego mira en el Dev UI qué ha cambiado.

## Pistas
<details><summary>Pista 1 (dirección)</summary>

Para la Tarea 3 piensa en el comando de la CLI para gestionar extensiones, o en el goal equivalente del plugin de Maven. Mira qué cambia en el `pom.xml` después.
</details>

<details><summary>Pista 2 (más concreta)</summary>

- Extensión: `quarkus extension add smallrye-health`, o `mvn quarkus:add-extension -Dextensions=smallrye-health`.
- Ruta con parámetro: `@Path("/{name}")` + `@PathParam("name")`.
- Si dev mode ya está corriendo, no hace falta reiniciarlo tras añadir la extensión.
</details>

## Autoevaluación
Responde sin mirar el código; deberías poder explicarlo en una entrevista.

1. ¿Qué diferencia hay entre el dev mode y ejecutar el jar? Nombra al menos tres cosas que cambian.
2. ¿Por qué no se declara la versión de `quarkus-rest` en el `pom.xml`?
3. ¿Qué hace el continuous testing y en qué se diferencia de ejecutar `mvn test` tú mismo?
4. Te piden que Kubernetes reinicie el pod si la aplicación se queda colgada. ¿Qué endpoint usarías y de qué extensión viene?
5. ¿Qué copiarías a un servidor para desplegar la aplicación?

La solución completa está en `solution/`.

## Retrospectiva
- ¿Qué entendí del ciclo de dev mode?
- ¿Qué me costó?
- ¿Qué reforzar?
