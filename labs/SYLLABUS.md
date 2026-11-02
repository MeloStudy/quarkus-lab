# 🗺️ Syllabus - Practical, up-to-date Quarkus

**Stack:** Quarkus 3.40.1.1 (LTS, supported until 2027-09-30) · Java 21 · Maven
**Quarkus 4:** still in beta (4.0.0.Beta2); it will be evaluated once it is stable and LTS.

**Format:** short labs (1-2 h). Two types depending on the topic:
- **Type A (starter + solution):** code with behavior verifiable by tests. `starter/` has TODOs and failing tests; `solution/` is the reference.
- **Type B (guide):** operational or exploratory topics (containers, native, Kubernetes, observability). A single project and a step-by-step guide with commands and "what you should see"; no separate solution.
- **Project (P1, P2, P3):** 3-6 h. You build a small service from a written specification with acceptance tests, using what you learned so far. No TODO per task, no step-by-step; the solution is a reference, not a script.

**Feedback:** per lab and asynchronous. The learner solves a lab, comments what could improve, and the improvements are applied to that lab, to the earlier ones and to the labs still to be built. Building never waits for feedback; the agent reminds the learner when a block is finished.

**Status:** ⬜ pending · 🟨 built, waiting for the learner · ✅ done

**Core / elective:** labs marked *(elective)* are valuable but skippable if time is short; everything else is the job-level core path.

> Draft. Order and topics are adjusted according to how the labs turn out.

## Design rules (agreed with the learner)
1. **Starting point = the smallest app that makes the topic visible.** A lab continues the previous domain only when that domain *is* the topic (REST, validation, persistence). Otherwise it starts from a minimal app (one resource, in-memory data, no Docker unless the topic needs it). Spending time understanding a domain instead of the topic is a bug.
2. **No single application grows through all the labs.** Integration happens in projects (P1, P2, P3), where the learner applies several topics to a new, small domain.
3. **Every topic has one owner lab.** Other labs may touch it but must refer to the owner instead of re-teaching it (see "Owns / builds on").
4. Labs 08-10 were slimmed to follow rule 1: they start from the REST + validation + error contract of labs 04-05 and carry no REST Client, price endpoint or OpenAPI layer (labs 06 and 07 own those).

## Block 1 - Fundamentals
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| [01](lab-01-project-dev-mode/README.md) | Project and dev mode | Structure, live coding, Dev UI, extensions, `quarkus` CLI, running the jar | Owns dev mode, extensions. Health only as an example (24 owns it) | Generated project | 🟨 |
| [02](lab-02-configuration-profiles/README.md) | Configuration and profiles | `@ConfigProperty`, `@ConfigMapping`, profiles, precedence, fail-fast validation, jar and build-time vs runtime | Owns configuration basics | Minimal app | 🟨 |
| [03](lab-03-cdi/README.md) | CDI | Scopes, qualifiers, producers, interceptors, events, unused bean removal | Owns CDI and ArC | Minimal app | 🟨 |

## Block 2 - REST
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| [04](lab-04-quarkus-rest/README.md) | Quarkus REST | Resources, JSON, status codes, `Location`, parameters, paging, response filters, resource lifecycle | Owns the REST basics | Catalog API (in memory) | 🟨 |
| [05](lab-05-validation-errors/README.md) | Validation and errors | Hibernate Validator, custom constraints, `@ServerExceptionMapper`, one error contract, catch-all without leaks | Owns validation and the error contract | Lab 04 | 🟨 |
| [06](lab-06-openapi/README.md) | OpenAPI | Generated document, annotations, reusable error responses, `OASFilter`, Swagger UI vs production | Owns the API contract | Lab 05 | 🟨 |
| [07](lab-07-rest-client/README.md) | REST Client | `@RegisterRestClient(configKey)`, URL and timeouts per profile, translating remote failures (404, 5xx, timeout, garbage, down) into the `ApiError` contract, testing against a stub server | Owns the client and its timeouts. Intro to test doubles | Lab 06 | 🟨 |

## Block 3 - Persistence
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| [08](lab-08-panache/README.md) | Hibernate ORM with Panache | Repository vs active record, Dev Services, paging in the database, uniqueness as a constraint (409), same-transaction writes, running the jar against a real PostgreSQL | Owns Panache and Dev Services (intro) | Catalog API of labs 04-05 (no OpenAPI, no REST Client) | 🟨 |
| [09](lab-09-relationships/README.md) | Relationships and inheritance | `@ManyToMany` and `@OneToMany`/`@ManyToOne` (owning vs inverse side), `SINGLE_TABLE` inheritance, `LazyInitializationException`, N+1, cascade vs bulk delete | Owns mapping | Lab 08 | 🟨 |
| [10](lab-10-queries-transactions/README.md) | Queries and transactions | Safe dynamic queries (named parameters, `locate` vs `LIKE`), aggregates with `select new`, rollback rules (checked exceptions), `REQUIRES_NEW`, lost updates and pessimistic locking | Owns queries and transactions | Lab 09 | 🟨 |
| [11](lab-11-migrations/README.md) | Migrations | Flyway: versioned and repeatable migrations, `validate`, a V1 generated from the entities, SQL-level constraints Hibernate cannot declare (`lower(name)` index, `CHECK`s), a migration that rewrites existing data (tested against legacy rows), jar on a real PostgreSQL, checksum mismatch, baseline of an existing database | Owns the schema in production | Lab 10 solution (already slim) | 🟨 |
| [12](lab-12-dtos-mapstruct/README.md) | DTOs and MapStruct | Separating entity and API model (an internal `cost_price` column that must not leak), records as DTOs, MapStruct as a CDI bean (`unmappedTargetPolicy = ERROR`, `@SubclassMapping` for the review hierarchy, `@MappingTarget`), where to map relative to the transaction | Builds on 09 (lazy loading). MapStruct is outside the Quarkus BOM (justified in the README) | Lab 11 solution | 🟨 |
| [13](lab-13-cache-scheduler/README.md) | Cache and scheduled tasks | `quarkus-cache` (`@CacheResult`, `@CacheKey`, invalidation, TTL and size), the silent wrong-key invalidation, stale data behind the cache, stampede protection, failures not cached; `@Scheduled` with config expressions, `concurrentExecution`, sub-second intervals, the cluster caveat | Owns cache and scheduling | Minimal app with one slow lookup (no database) | 🟨 |

## Block 4 - REST for production
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| [14](lab-14-json/README.md) | JSON in depth | A reservations API contract: snake_case names, ISO `LocalDate`/`Instant`, enum codes with `@JsonValue`, strict input (unknown properties, datetime for a date), polymorphic payments with `Id.NAME` (and why not `Id.CLASS`), a custom `Money` (de)serializer registered with `ObjectMapperCustomizer` | Owns serialization | Minimal app (in memory, no database) | 🟨 |
| [15](lab-15-http/README.md) | HTTP for production | `ETag` + `If-None-Match` (304), `If-Match` over `@Version` (412, 428, the race and its mapper), CORS (origins, exposed headers, preflight), multipart upload and download (generated names, allow-listed types, size limit, the `@Produces` wildcard trap), v1/v2 side by side with `Deprecation` and `Sunset` | Builds on 10 (optimistic locking was a stretch there) | Minimal app with a `@Version` entity (Dev Services) | 🟨 |

## Block 5 - Testing
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| [16](lab-16-quarkus-test/README.md) | `@QuarkusTest` | `@InjectMock` (and its normal-scope rule), `QuarkusMock.installMockForType` with a fixed `Clock`, shared state that outlives a test, `@TestTransaction` and why HTTP cannot see its rows, a plain Mockito unit test next to the slow ones, what a `@TestProfile` costs. The learner completes the arrangement of finished test classes | Owns mocking | Minimal app (Dev Services for the database) | 🟨 |
| [17](lab-17-test-profiles/README.md) | Dev Services and test profiles | Dev Services configuration (pinned PostgreSQL image, init script for a table Hibernate does not own), a `QuarkusTestResourceLifecycleManager` that starts a fake remote service before the app and hands over its URL, `QuarkusTestProfile` (config overrides and attached resources), counting application starts (3, 4 with a copy profile class, 2 with a GLOBAL resource), container reuse. The learner completes the infrastructure of finished test classes | Owns test profiles (Dev Services intro was lab 08) | Minimal app with a database | 🟨 |
| [18](lab-18-service-tests/README.md) | Integration tests between services | WireMock (outside the BOM, justified) started by a lifecycle manager and injected into the test, stubs generated from a shared JSON contract, failure injection (500, garbage, missing field, connection reset, slow answer), provider-side contract verification with provider state, `@QuarkusIntegrationTest` against the packaged jar. Two Maven modules (`inventory`, `orders`). The learner completes the infrastructure of finished test classes | Owns WireMock and contracts | Two minimal services | 🟨 |
| **P1** | **Project 1 - a service from a specification** | REST + validation + persistence + migrations + DTOs + tests, new small domain, written spec and acceptance tests | Uses blocks 1-5 | Empty project | ⬜ |

## Block 6 - Security
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| 19 | Authentication and roles | Basic auth, `@RolesAllowed`, identity in tests (`@TestSecurity`) | Owns authorization basics | Minimal app | ⬜ |
| 20 | JWT | SmallRye JWT: verifying tokens, claims, roles from a token, token in tests | Owns JWT | Minimal app | ⬜ |
| 21 | OIDC and Keycloak | OIDC service apps, Keycloak with Dev Services, token propagation | Builds on 20 | Minimal app | ⬜ |
| 22 | TLS and secrets | HTTPS, security headers, secrets out of the repo and out of the jar | Owns secrets (builds on 02) | Minimal app | ⬜ |

## Block 7 - Resilience and observability
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| 23 | Fault tolerance | Retry, timeout, circuit breaker, fallback, bulkhead | Owns resilience; builds on the REST Client and timeouts of 07 | Minimal app with a flaky remote (stub of 07) | ⬜ |
| 24 | Health and metrics | SmallRye Health (liveness vs readiness), Micrometer, custom metrics | Owns health and metrics | Minimal app | ⬜ |
| 25 | Logging and tracing | Structured logging, correlation id, OpenTelemetry | Owns logging and tracing (the request id of 04 is the starting idea) | Minimal app | ⬜ |
| **P2** | **Project 2 - a service that is secure and observable** | A service that calls a remote dependency: authentication, resilience, metrics, tracing, health, with a written spec and acceptance tests | Uses blocks 6-7 (and 3 if it needs data) | Empty project | ⬜ |

## Block 8 - Reactive and communication
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| 26 | Execution model | Event loop vs worker threads, `@Blocking`/`@NonBlocking`, virtual threads (`@RunOnVirtualThread`) | Owns the execution model | Minimal app | ⬜ |
| 27 | Mutiny | `Uni`, `Multi`, composition, errors, backpressure basics | Owns Mutiny | Minimal app | ⬜ |
| 28 | Reactive messaging (in memory) | SmallRye Reactive Messaging channels, acknowledgement, failure handling | Owns messaging concepts | Minimal app | ⬜ |
| 29 | Kafka *(elective)* | Kafka with Dev Services, serialization, consumer groups, dead-letter, outbox idea | Builds on 28 and on 10 (transactions) | Minimal app | ⬜ |
| 30 | gRPC *(elective)* | Defining `.proto`, gRPC server and client, streaming | Owns gRPC | Minimal app | ⬜ |

## Block 9 - Maintaining and delivering
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| 31 | Multi-module projects and Jandex | Library modules, bean discovery across jars (Jandex index, `beans.xml`), shared BOM | Owns project structure | Two modules | ⬜ |
| 32 | Upgrading Quarkus | `quarkus update`, release notes and migration guides, LTS vs regular, preparing for Quarkus 4 | Owns upgrades | Project on an older platform version | ⬜ |
| 33 | Containers | Dockerfiles (JVM), build with Jib/Docker, sizing and layers | Owns images | Minimal app | ⬜ |
| 34 | Native *(elective)* | GraalVM/Mandrel, limitations, reflection (`@RegisterForReflection`) | Owns native | Minimal app | ⬜ |
| 35 | Kubernetes and configuration | Kubernetes extension, generated manifests, ConfigMap and Secret, probes (from 24) | Builds on 22, 24 and 33 | Minimal app | ⬜ |
| 36 | CI with GitHub Actions | Pipeline: build, tests, image; Maven cache | Owns CI | Minimal app | ⬜ |

## Block 10 - Capstone
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| **P3** | **Capstone project** | A complete service in three milestones: (1) API, persistence, migrations; (2) security, resilience, observability; (3) container, CI and a production checklist | Uses everything | Empty project | ⬜ |

## Notes for the next labs
- **Block 5 (testing) labs 16-18 are built; P1 closes it.** P1 is the first project: an empty project, a written specification and a visible acceptance suite without hints, using blocks 1-5 (REST, validation, persistence, Flyway, DTOs, tests). Decide the domain when starting it. Lab 18 introduced a multi-module layout (`inventory` + `orders` + `contracts/`) and a `verify.ps1` that runs `verify -DskipITs=false` when a variant has `*IT.java` and collects reports from every module (`surefire-reports` and `failsafe-reports`).
- **Projects (P1, P2, P3):** the first one needs a written specification and an acceptance test suite that is visible but has no hints. Decide the domain when the block before it is finished.
- **Cross references inside READMEs** (for example "lab 26" in lab 01) must follow this numbering; `grep` for `lab [0-9]` when the numbering changes.

## Pace
One lab at a time: it is built and verified; the learner solves and reviews it whenever they can; feedback is applied to everything built so far and to what comes next.
