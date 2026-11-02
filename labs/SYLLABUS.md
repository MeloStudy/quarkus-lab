# Sílabo - Quarkus práctico y actualizado

**Stack:** Quarkus 3.40.1.1 (LTS, soporte hasta 30-09-2027) · Java 21 · Maven
**Quarkus 4:** aún en beta (4.0.0.Beta2); se evaluará cuando sea estable y LTS.
**Formato:** labs cortos (1-2 h), cada uno con `starter/` (con TODOs y tests fallando) y `solution/`.
**Estado:** ⬜ pendiente · 🟨 en curso · ✅ terminado

> Borrador. El orden y los temas se ajustan según cómo vayan saliendo los labs.

## Bloque 1 - Fundamentos
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 01 | Proyecto y dev mode | Estructura, live coding, Dev UI, extensiones, `quarkus` CLI | 🟨 |
| 02 | Configuración y perfiles | `@ConfigProperty`, `@ConfigMapping`, perfiles, variables de entorno | ⬜ |
| 03 | CDI | Scopes, qualifiers, producers, interceptors, eventos | ⬜ |

## Bloque 2 - REST
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 04 | Quarkus REST | Resources, JSON, códigos de estado, parámetros | ⬜ |
| 05 | Validación y errores | Hibernate Validator, `ExceptionMapper`, respuestas de error consistentes | ⬜ |
| 06 | OpenAPI | Documentar la API, Swagger UI | ⬜ |
| 07 | REST Client | Consumir otra API con `@RegisterRestClient` | ⬜ |
| 08 | DTOs y MapStruct | Separar entidad y DTO, mapeo con MapStruct (`componentModel = cdi`) | ⬜ |

## Bloque 3 - Persistencia
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 09 | Hibernate ORM con Panache | Active record vs repository, Dev Services | ⬜ |
| 10 | Relaciones y herencia | `@OneToMany`, `@ManyToMany`, estrategias de herencia | ⬜ |
| 11 | Consultas y transacciones | Paginación, queries, `@Transactional` | ⬜ |
| 12 | Migraciones | Flyway | ⬜ |
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
| 17 | Mutiny | `Uni`, `Multi`, composición, errores | ⬜ |
| 18 | Mensajería reactiva | SmallRye Reactive Messaging (in-memory, luego Kafka) | ⬜ |
| 19 | gRPC | Definir `.proto`, servidor y cliente gRPC, streaming | ⬜ |

## Bloque 6 - Seguridad
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 20 | Autenticación y roles | Basic, `@RolesAllowed`, identidad en tests | ⬜ |
| 21 | JWT y OIDC | SmallRye JWT, Keycloak con Dev Services | ⬜ |

## Bloque 7 - Resiliencia y observabilidad
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 22 | Fault tolerance | Retry, timeout, circuit breaker, fallback | ⬜ |
| 23 | Health y métricas | SmallRye Health, Micrometer | ⬜ |
| 24 | Logging y tracing | Logging estructurado, OpenTelemetry | ⬜ |

## Bloque 8 - Entrega
| Lab | Tema | Qué se practica | Estado |
|-----|------|-----------------|--------|
| 25 | Contenedores | Dockerfiles (JVM), build con Jib/Docker | ⬜ |
| 26 | Native | GraalVM/Mandrel, limitaciones, reflexión | ⬜ |
| 27 | Kubernetes | Extensión de Kubernetes, manifiestos generados | ⬜ |

## Ritmo
Un lab a la vez: se construye, se resuelve, se revisa y se ajusta el siguiente según la retrospectiva.
