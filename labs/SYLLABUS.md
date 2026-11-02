# Sílabo - Quarkus práctico y actualizado

**Stack:** Quarkus 3.40.1.1 (LTS, soporte hasta 30-09-2027) · Java 21 · Maven
**Quarkus 4:** aún en beta (4.0.0.Beta2); se evaluará cuando sea estable y LTS.
**Formato:** labs cortos (1-2 h). Dos tipos según el tema:
- **Tipo A (starter + solution):** código con comportamiento verificable por tests. `starter/` tiene TODOs y tests fallando; `solution/` es la referencia.
- **Tipo B (guía):** temas operativos o exploratorios (contenedores, native, Kubernetes, observabilidad). Un solo proyecto y una guía paso a paso con comandos y "qué deberías ver"; sin solución separada.

**Primer lote (labs 01-05):** 01 proyecto y dev mode · 02 configuración · 03 CDI · 04 Quarkus REST · 05 validación y errores.
**Estado:** ⬜ pendiente · 🟨 en curso · ✅ terminado

> Borrador. El orden y los temas se ajustan según cómo vayan saliendo los labs.

## Bloque 1 - Fundamentos
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 01 | Proyecto y dev mode | Estructura, live coding, Dev UI, extensiones, `quarkus` CLI | 🟨 |
| 02 | Configuración y perfiles | `@ConfigProperty`, `@ConfigMapping`, perfiles, precedencia, validación fail-fast, jar y build-time vs runtime | 🟨 |
| 03 | CDI | Scopes, qualifiers, producers, interceptors, eventos | ⬜ |

## Bloque 2 - REST
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 04 | Quarkus REST | Resources, JSON, códigos de estado, parámetros | ⬜ |
| 05 | Validación y errores | Hibernate Validator, `ExceptionMapper`, respuestas de error consistentes | ⬜ |
| 06 | OpenAPI | Documentar la API, Swagger UI | ⬜ |
| 07 | REST Client | Consumir otra API con `@RegisterRestClient` | ⬜ |

## Bloque 3 - Persistencia
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 08 | Hibernate ORM con Panache | Active record vs repository, Dev Services (introducción) | ⬜ |
| 09 | Relaciones y herencia | `@OneToMany`, `@ManyToMany`, estrategias de herencia | ⬜ |
| 10 | Consultas y transacciones | Paginación, queries, `@Transactional` | ⬜ |
| 11 | Migraciones | Flyway | ⬜ |
| 12 | DTOs y MapStruct | Separar entidad y DTO, mapeo con MapStruct (`componentModel = cdi`) | ⬜ |
| 13 | Caché y tareas programadas | `quarkus-cache`, `@Scheduled` | ⬜ |

## Bloque 4 - Testing
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 14 | `@QuarkusTest` | RestAssured, mocks, `@InjectMock` | ⬜ |
| 15 | Dev Services y perfiles de test | Contenedores en test, `QuarkusTestProfile` | ⬜ |
| 16 | Pruebas de integración entre servicios | `@QuarkusIntegrationTest`, WireMock, contratos entre servicios | ⬜ |

## Bloque 5 - Reactivo y comunicación
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 17 | Modelo de ejecución | Event loop vs worker threads, `@Blocking`/`@NonBlocking`, virtual threads (`@RunOnVirtualThread`) | ⬜ |
| 18 | Mutiny | `Uni`, `Multi`, composición, errores | ⬜ |
| 19 | Mensajería reactiva | SmallRye Reactive Messaging (in-memory, luego Kafka) | ⬜ |
| 20 | gRPC | Definir `.proto`, servidor y cliente gRPC, streaming | ⬜ |

## Bloque 6 - Seguridad
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 21 | Autenticación y roles | Basic, `@RolesAllowed`, identidad en tests | ⬜ |
| 22 | JWT y OIDC | SmallRye JWT, Keycloak con Dev Services | ⬜ |

## Bloque 7 - Resiliencia y observabilidad
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 23 | Fault tolerance | Retry, timeout, circuit breaker, fallback | ⬜ |
| 24 | Health y métricas | SmallRye Health, Micrometer | ⬜ |
| 25 | Logging y tracing | Logging estructurado, OpenTelemetry | ⬜ |

## Bloque 8 - Entrega
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 26 | Contenedores | Dockerfiles (JVM), build con Jib/Docker | ⬜ |
| 27 | Native | GraalVM/Mandrel, limitaciones, reflexión | ⬜ |
| 28 | Kubernetes | Extensión de Kubernetes, manifiestos generados | ⬜ |
| 29 | CI con GitHub Actions | Pipeline: build, tests, imagen; caché de Maven | ⬜ |

## Bloque 9 - Integrador
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 30 | Proyecto integrador | Un servicio completo desde cero: REST + persistencia + seguridad + resiliencia + observabilidad + contenedor + CI, con checklist de producción | ⬜ |

## Ritmo
Un lab a la vez: se construye, se resuelve, se revisa y se ajusta el siguiente según la retrospectiva.
