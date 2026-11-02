# 🗺️ Syllabus - Practical, up-to-date Quarkus

**Stack:** Quarkus 3.40.1.1 (LTS, supported until 2027-09-30) · Java 21 · Maven
**Quarkus 4:** still in beta (4.0.0.Beta2); it will be evaluated once it is stable and LTS.

**Format:** short labs (1-2 h). Two types depending on the topic:
- **Type A (starter + solution):** code with behavior verifiable by tests. `starter/` has TODOs and failing tests; `solution/` is the reference.
- **Type B (guide):** operational or exploratory topics (containers, native, Kubernetes, observability). A single project and a step-by-step guide with commands and "what you should see"; no separate solution.

**First batch (labs 01-05):** 01 project and dev mode · 02 configuration · 03 CDI · 04 Quarkus REST · 05 validation and errors.

**Status:** ⬜ pending · 🟨 built, waiting for the learner · ✅ done

> Draft. Order and topics are adjusted according to how the labs turn out.

## Block 1 - Fundamentals
| Lab | Topic | What is practiced | Status |
|-----|-------|-------------------|--------|
| [01](lab-01-project-dev-mode/README.md) | Project and dev mode | Structure, live coding, Dev UI, extensions, `quarkus` CLI, running the jar | 🟨 |
| [02](lab-02-configuration-profiles/README.md) | Configuration and profiles | `@ConfigProperty`, `@ConfigMapping`, profiles, precedence, fail-fast validation, jar and build-time vs runtime | 🟨 |
| [03](lab-03-cdi/README.md) | CDI | Scopes, qualifiers, producers, interceptors, events, unused bean removal | 🟨 |

## Block 2 - REST
| Lab | Topic | What is practiced | Status |
|-----|-------|-------------------|--------|
| [04](lab-04-quarkus-rest/README.md) | Quarkus REST | Resources, JSON, status codes, `Location`, parameters, paging, response filters, resource lifecycle | 🟨 |
| [05](lab-05-validation-errors/README.md) | Validation and errors | Hibernate Validator, custom constraints, `@ServerExceptionMapper`, one error contract, catch-all without leaks | 🟨 |
| 06 | OpenAPI | Documenting the API, Swagger UI | ⬜ |
| 07 | REST Client | Consuming another API with `@RegisterRestClient` | ⬜ |

## Block 3 - Persistence
| Lab | Topic | What is practiced | Status |
|-----|-------|-------------------|--------|
| 08 | Hibernate ORM with Panache | Active record vs repository, Dev Services (introduction) | ⬜ |
| 09 | Relationships and inheritance | `@OneToMany`, `@ManyToMany`, inheritance strategies | ⬜ |
| 10 | Queries and transactions | Pagination, queries, `@Transactional` | ⬜ |
| 11 | Migrations | Flyway | ⬜ |
| 12 | DTOs and MapStruct | Separating entity and DTO, mapping with MapStruct (`componentModel = cdi`) | ⬜ |
| 13 | Cache and scheduled tasks | `quarkus-cache`, `@Scheduled` | ⬜ |

## Block 4 - Testing
| Lab | Topic | What is practiced | Status |
|-----|-------|-------------------|--------|
| 14 | `@QuarkusTest` | RestAssured, mocks, `@InjectMock` | ⬜ |
| 15 | Dev Services and test profiles | Containers in tests, `QuarkusTestProfile` | ⬜ |
| 16 | Integration tests between services | `@QuarkusIntegrationTest`, WireMock, contracts between services | ⬜ |

## Block 5 - Reactive and communication
| Lab | Topic | What is practiced | Status |
|-----|-------|-------------------|--------|
| 17 | Execution model | Event loop vs worker threads, `@Blocking`/`@NonBlocking`, virtual threads (`@RunOnVirtualThread`) | ⬜ |
| 18 | Mutiny | `Uni`, `Multi`, composition, errors | ⬜ |
| 19 | Reactive messaging | SmallRye Reactive Messaging (in-memory, then Kafka) | ⬜ |
| 20 | gRPC | Defining `.proto`, gRPC server and client, streaming | ⬜ |

## Block 6 - Security
| Lab | Topic | What is practiced | Status |
|-----|-------|-------------------|--------|
| 21 | Authentication and roles | Basic, `@RolesAllowed`, identity in tests | ⬜ |
| 22 | JWT and OIDC | SmallRye JWT, Keycloak with Dev Services | ⬜ |

## Block 7 - Resilience and observability
| Lab | Topic | What is practiced | Status |
|-----|-------|-------------------|--------|
| 23 | Fault tolerance | Retry, timeout, circuit breaker, fallback | ⬜ |
| 24 | Health and metrics | SmallRye Health, Micrometer | ⬜ |
| 25 | Logging and tracing | Structured logging, OpenTelemetry | ⬜ |

## Block 8 - Delivery
| Lab | Topic | What is practiced | Status |
|-----|-------|-------------------|--------|
| 26 | Containers | Dockerfiles (JVM), build with Jib/Docker | ⬜ |
| 27 | Native | GraalVM/Mandrel, limitations, reflection | ⬜ |
| 28 | Kubernetes | Kubernetes extension, generated manifests | ⬜ |
| 29 | CI with GitHub Actions | Pipeline: build, tests, image; Maven cache | ⬜ |

## Block 9 - Capstone
| Lab | Topic | What is practiced | Status |
|-----|-------|-------------------|--------|
| 30 | Capstone project | A complete service from scratch: REST + persistence + security + resilience + observability + container + CI, with a production checklist | ⬜ |

## Pace
One lab at a time: it is built, solved, reviewed, and the next one is adjusted according to the retrospective.
