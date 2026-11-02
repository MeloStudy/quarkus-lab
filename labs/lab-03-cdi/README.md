# Lab 03 - CDI (inyección de dependencias)

**Duración estimada:** 2 h · **Quarkus:** 3.40.1.1 LTS · **Java:** 21 · **Tipo:** A (starter + solution)

## Por qué importa
Todo en Quarkus es un bean: resources, servicios, repositorios, clientes. Entender **scopes, qualifiers, interceptors y eventos** evita bugs de estado compartido, ambigüedades y "beans que desaparecen". Además, Quarkus resuelve la inyección **en tiempo de build** (ArC), no en ejecución: es distinto de Spring y es tema típico de entrevista.

## Conceptos
- Scopes: `@ApplicationScoped`, `@RequestScoped`, `@Singleton`, `@Dependent`; client proxies
- Inyección por constructor (recomendada) y `Instance<T>` / `@Any`
- Qualifiers (con miembros) y resolución ambigua
- Producers y `@DefaultBean`; sustitución de beans en tests con `@Alternative` + `@Priority`
- Interceptors (`@InterceptorBinding`, `@AroundInvoke`)
- Eventos: `Event<T>`, `@Observes`, `@ObservesAsync`, prioridad
- Eliminación de beans sin uso y `@Unremovable`

## Documentación
- [CDI en Quarkus (introducción)](https://quarkus.io/guides/cdi)
- [Referencia de CDI en Quarkus (ArC)](https://quarkus.io/guides/cdi-reference)

> Nota: las guías públicas muestran la última versión; tu proyecto usa 3.40.1.1 (LTS), que coincide.

## Escenario
`starter/` es un pequeño sistema de pedidos: `OrderService` registra pedidos, hay notificadores por canal, contadores y un exportador. Casi todo compila y arranca, pero **11 de los 12 tests fallan**. Cada tarea arregla un grupo de tests. Trabaja desde `starter/` y ejecuta los tests con `.\mvnw.cmd test` (o `mvn test`).

## Tareas

### Tarea 1 - Scopes (`ScopesTest`)
`HitWriter` escribe en un `HitCounter` y `HitReader` lo lee; `DemoResource` devuelve un id por petición.
1. [ ] **Antes de ejecutar, predice:** con `HitCounter` en `@Dependent`, ¿ve `HitReader` los golpes que escribe `HitWriter`? ¿Y qué clase real tiene el objeto `HitCounter` que inyecta el test?
2. [ ] Ejecuta `ScopesTest` y contrasta tu predicción con los mensajes de fallo.
3. [ ] Corrige los scopes de `HitCounter` y de `RequestContextInfo` para que los 3 tests pasen. Fíjate en el nombre de la clase del objeto inyectado una vez corregido.

### Tarea 2 - Qualifiers e `Instance` (`NotificationServiceTest`)
1. [ ] En `NotificationService`, inyecta **por constructor** un único `Notifier` (sin qualifier) y ejecuta `.\mvnw.cmd package -DskipTests`. **Predice** el resultado y luego lee el error completo: ¿en qué fase se detecta, en build o al ejecutar?
2. [ ] Crea un qualifier `@Channel` con un `enum Type { EMAIL, SMS }` como miembro, y aplícalo a `EmailNotifier` y `SmsNotifier`.
3. [ ] Inyecta `@Channel(EMAIL) Notifier` y `@Channel(SMS) Notifier` para `emailTo` / `smsTo`.
4. [ ] Implementa `broadcast` con `@Any Instance<Notifier>`.
5. [ ] Ahora que los beans tienen `@Channel`, vuelve a inyectar un `Notifier` **sin** qualifier. ¿Sigue siendo ambiguo el error? Explica el cambio.

### Tarea 3 - Producer y `@DefaultBean` (`ClockProducerTest`)
`OrderService` usa `Instant.now()`: imposible de probar. El test ya trae un `Clock` fijo (`src/test/.../FixedClockProducer`).
1. [ ] Crea un producer de `java.time.Clock` (reloj del sistema, UTC) marcado `@DefaultBean`.
2. [ ] Inyecta el `Clock` en `OrderService` y úsalo para `createdAt`.
3. [ ] Observa cómo el test sustituye el reloj sin tocar el código de producción. ¿Qué dos anotaciones lo consiguen?

### Tarea 4 - Interceptor (`AuditInterceptorTest`)
1. [ ] Crea el binding `@Audited` (`@InterceptorBinding`) y el interceptor que registra `Clase.método` en `AuditLog` antes de ejecutar y luego continúa la invocación.
2. [ ] Anota `OrderService.place` con `@Audited`.

### Tarea 5 - Eventos (`OrderEventsTest`)
1. [ ] Inyecta `Event<OrderPlaced>` en `OrderService` y dispáralo de forma síncrona y asíncrona al registrar el pedido.
2. [ ] En `OrderObservers`, haz de `shippingPrepared` y `stockReserved` observers síncronos, y que el **stock se ejecute antes que el envío** aunque esté declarado después (usa prioridad). Convierte `customerNotified` en observer asíncrono.

### Tarea 6 - Beans sin uso (`UnusedBeanTest`)
1. [ ] Ejecuta el test, lee el mensaje y averigua por qué `LegacyExporter` "no existe" aunque tiene `@ApplicationScoped`.
2. [ ] Corrígelo sin inyectar el bean en ningún sitio artificialmente.

### Tarea 7 - Experimentos (sin tests; **anota una predicción antes de cada uno**)
Hazlos en una copia temporal o revierte después.
1. [ ] **Autoinvocación:** añade a `OrderService` un método público `@Audited void validate(String item)` y llámalo desde `place` con `validate(item)`. ¿Cuántas entradas aparecen en `AuditLog`?
2. [ ] **Excepción en un observer síncrono:** haz que `stockReserved` lance una excepción. ¿Qué pasa con `place()`? ¿Se ejecutan los demás observers?
3. [ ] **Excepción en un observer asíncrono:** igual con `customerNotified`. ¿Se entera quien llamó a `place()`?
4. [ ] **Request scope fuera de petición:** desde un `new Thread(...)` en un test, llama a `requestInfo.id()`. ¿Qué ocurre?

**Criterio de éxito:** los 12 tests pasan en `starter/` (`.\mvnw.cmd verify`) y has hecho la Tarea 7.

## Qué deberías ver
- Tras la Tarea 1, el objeto inyectado es un proxy: `lab.melostudy.HitCounter_ClientProxy`.
- Al final, `ScopesTest`, `NotificationServiceTest`, `ClockProducerTest`, `AuditInterceptorTest`, `OrderEventsTest` y `UnusedBeanTest` en verde (12 tests).

## En proyectos reales
- **Constructor injection** (como en este lab): dependencias explícitas, campos `final`, y las pruebas unitarias pueden construir la clase sin contenedor. Quarkus permite omitir `@Inject` si hay un solo constructor.
- **Errores de inyección = fallo de build.** Dependencias ambiguas o no satisfechas se detectan en `package`, no en producción. Una CI que compila ya valida el grafo de beans.
- **`@ApplicationScoped` por defecto** para servicios sin estado; **`@Singleton`** si no necesitas proxy (más simple y barato; es lo que usa el producer de `Clock` de este lab); **`@Dependent`** solo para objetos de vida corta ligados a quien los usa; **`@RequestScoped`** para datos de la petición (usuario, correlation id). Estado mutable compartido en `@ApplicationScoped` debe ser thread-safe.
- **Sustituir beans en tests:** `@DefaultBean` en producción + `@Alternative`/`@Priority` en `src/test`, o `@InjectMock` (lab 14).
- **Interceptors para preocupaciones transversales** (auditoría, métricas, reintentos). Ojo: el código del interceptor se ejecuta en cada llamada; mantenlo barato. Las anotaciones de Quarkus como `@Transactional` o `@Retry` son interceptors.
- **Eventos para desacoplar**, no para flujos críticos: un observer síncrono que falla rompe a quien dispara; uno asíncrono falla en silencio (hay que registrar o gestionar el error dentro del observer). Si necesitas garantías de entrega, usa mensajería (lab 19).
- **En entrevista (Spring vs Quarkus):** en Spring la autoinvocación no pasa por el proxy y no se aplican aspectos; en Quarkus (ArC) sí se interceptan. Y Quarkus hace la DI en build time: no hay reflexión ni escaneo en el arranque, lo que reduce memoria y tiempo de arranque (clave para native, lab 27).

## Reto extra
Selecciona el notificador **dinámicamente** por nombre de canal (`String`) usando `Instance.select(...)` con un `AnnotationLiteral<Channel>`. Pista: el literal debe devolver el valor del miembro del qualifier.

## Pistas
<details><summary>Pista 1 (dirección)</summary>

- Tarea 1: piensa cuántas instancias crea el contenedor por cada punto de inyección en cada scope.
- Tarea 2: el qualifier necesita `@Qualifier`, retención en runtime y un miembro de enum.
- Tarea 3: mira `io.quarkus.arc.DefaultBean` y cómo un producer declara su scope.
- Tarea 6: busca en la guía `cdi-reference` la sección sobre eliminación de beans sin usar.
</details>

<details><summary>Pista 2 (más concreta)</summary>

- Scopes: `HitCounter` → `@ApplicationScoped`; `RequestContextInfo` → `@RequestScoped`.
- Qualifier: `@Qualifier @Retention(RUNTIME) @Target({TYPE, METHOD, FIELD, PARAMETER}) @interface Channel { Type value(); }`. En el constructor: `@Channel(Channel.Type.EMAIL) Notifier email`.
- Producer: `@Produces @Singleton @DefaultBean Clock clock() { return Clock.systemUTC(); }` dentro de un bean.
- Interceptor: `@Audited @Interceptor @Priority(Interceptor.Priority.APPLICATION)` + método con `@AroundInvoke` que recibe `InvocationContext` y llama a `proceed()`.
- Eventos: `@Observes @Priority(1000)` (menor número = antes); `@ObservesAsync`; `fire(...)` y `fireAsync(...)` son canales distintos.
- Bean sin uso: `@io.quarkus.arc.Unremovable`.
</details>

## Respuestas verificadas
<details><summary>Abrir solo después de hacer las tareas</summary>

Todo esto se comprobó ejecutándolo con Quarkus 3.40.1.1.

- **Tarea 1:** con `@Dependent`, cada punto de inyección recibe **su propia instancia**; `HitReader` ve 0 aunque el writer escriba. Con `@ApplicationScoped` hay una sola instancia compartida, y lo que se inyecta es un **client proxy** (`HitCounter_ClientProxy`) que delega en ella. Con `@RequestScoped` el id cambia en cada petición HTTP.
- **Tarea 2.1:** falla en **build** (`package`), no al ejecutar: `AmbiguousResolutionException: Ambiguous dependencies for type lab.melostudy.Notifier and qualifiers [@Default]`, con los dos beans candidatos listados.
- **Tarea 2.5:** al calificar los beans con `@Channel` pierden `@Default`. Inyectar `Notifier` sin qualifier pasa a ser **no satisfecho**: `UnsatisfiedResolutionException: Unsatisfied dependency for type lab.melostudy.Notifier and qualifiers [@Default]`. Sigue sin funcionar, pero por la causa contraria. `@Any Instance<Notifier>` sí los ve todos.
- **Tarea 3.3:** `@Alternative` + `@Priority(1)`. Un alternative con prioridad gana a los beans normales y a los `@DefaultBean`.
- **Tarea 4:** en esta versión el interceptor también funciona si se omite `@Priority`; aun así conviene declararlo para fijar el orden cuando hay varios interceptors sobre el mismo método.
- **Tarea 6:** ArC elimina los beans que nadie inyecta ni observa y que no tienen interceptors (`quarkus.arc.remove-unused-beans`, por defecto activo). La búsqueda programática (`Arc.container().instance(...)`) no cuenta como uso. Solución: `@Unremovable`. Fíjate en que `OrderObservers` no se elimina porque tiene observers.
- **Tarea 7.1:** `[OrderService.place, OrderService.validate]`: **2 entradas**. ArC sí intercepta la autoinvocación.
- **Tarea 7.2:** `place()` lanza la excepción al llamador (`IllegalStateException: sync boom`). Los observers posteriores (incluido el asíncrono, que se dispara después) no llegan a ejecutarse.
- **Tarea 7.3:** `place()` **retorna normalmente**; el fallo no llega al llamador.
- **Tarea 7.4:** `ContextNotActiveException: RequestScoped context was not active when trying to obtain a bean instance for a client proxy of CLASS bean [class=lab.melostudy.RequestContextInfo ...]`. En el hilo del test funciona porque `@QuarkusTest` activa el contexto de petición. Las tareas en hilos propios (executors, `@Scheduled`) no lo tienen: usa `@ActivateRequestContext`.
</details>

## Autoevaluación
Responde sin mirar el código; deberías poder explicarlo en una entrevista.

1. ¿Qué diferencia hay entre `@ApplicationScoped`, `@Singleton` y `@Dependent`? ¿Qué es un client proxy y por qué lo necesitan los scopes normales?
2. Dos beans inyectan la misma interfaz y el proyecto no compila. ¿Cuándo falla (build o ejecución) y cómo lo resuelves de tres formas distintas?
3. Un bean `@ApplicationScoped` que nadie inyecta no aparece al buscarlo por `Arc.container()` o `CDI.current()`. ¿Por qué ocurre y cómo se arregla?
4. ¿Cómo sustituyes en un test un bean de producción por otro sin tocar el código principal? Nombra dos mecanismos.
5. Compara `@Observes` y `@ObservesAsync` respecto a hilo de ejecución, orden y propagación de errores. ¿Cuándo usarías mensajería en vez de eventos CDI?

La solución completa está en `solution/`.

## Retrospectiva
- ¿Qué entendí de CDI en Quarkus?
- ¿Qué me costó?
- ¿Qué reforzar?
