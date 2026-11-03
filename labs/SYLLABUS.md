# 🗺️ Syllabus - Practical, up-to-date Quarkus

**Stack:** Quarkus 3.40.1.1 (LTS, supported until 2027-09-30) · Java 21 · Maven
**Quarkus 4:** still in beta (4.0.0.Beta2); it will be evaluated once it is stable and LTS.

**Format:** short labs (1-2 h). Two types depending on the topic:
- **Type A (starter + solution):** code with behavior verifiable by tests. `starter/` has TODOs and failing tests; `solution/` is the reference.
- **Type B (guide):** operational or exploratory topics (containers, native, Kubernetes, observability). A single project and a step-by-step guide with commands and "what you should see"; no separate solution.
- **Project (P1, P2, P3):** a finished, working showcase of a small service that applies what the blocks taught, with a written specification and a guided review (`REVIEW.md`: reading order, the reason behind each decision, traps found while building it, experiments to break it). No starter, no TODOs: you read it, run it, break it and challenge it in the feedback session. (Decided with the learner on 2026-11-02: projects are not question-and-answer exercises.)

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
| [P1](project-1-issue-tracker/README.md) | **Project 1 - an issue tracker (showcase)** | A finished service: projects, issues numbered per project under concurrency, a status state machine, comments, an audit trail written in the same transaction, `ETag`/`If-Match`, a safe search with a whitelisted sort and no N+1, Flyway with database-level CHECKs, one error contract. 72 tests, a jar run against PostgreSQL, and a guided review with five break-it-yourself experiments | Uses blocks 1-5 | Built from scratch by the agent (no starter) | 🟨 |

## Block 6 - Security
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| [19](lab-19-auth-roles/README.md) | Authentication and roles | HTTP Basic with the embedded realm (dev/test only), 401 vs 403 vs 404, `@RolesAllowed` / `@Authenticated` / `@PermitAll`, a closed default (`deny-unannotated-endpoints`), ownership rules written against the `SecurityIdentity` (no mass assignment), path policies for a static file, `@TestSecurity` and what it cannot prove, and what the packaged app does in `prod` without a provider (500 everywhere) | Owns authorization basics | Minimal app (reports API, in memory, no Docker) | 🟨 |
| [20](lab-20-jwt/README.md) | JWT | SmallRye JWT: verifying tokens (key, issuer, audience, lifetime), claims, `groups` as roles, tokens and `@TestSecurity`/`@JwtSecurity` in tests | Owns JWT | Minimal app | 🟨 |
| [21](lab-21-oidc-keycloak/README.md) | OIDC and Keycloak | `quarkus-oidc` service app, Keycloak with Dev Services and a realm file, realm roles, audience, token propagation, `@OidcSecurity` (needs Docker) | Builds on 20 | Minimal app | 🟨 |
| [22](lab-22-tls-secrets/README.md) | TLS and secrets | HTTPS with a PKCS12 keystore (TLS registry), redirect, TLS 1.3 default, security headers, secrets out of the repo and out of the jar, jar run with environment and mounted file | Owns secrets (builds on 02) | Minimal app | 🟨 |

## Block 7 - Resilience and observability
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| [23](lab-23-fault-tolerance/README.md) | Fault tolerance | Timeout, retry (what to retry), fallback (stale, never invented), circuit breaker (window, `failOn`, config override), bulkhead; a scripted carrier stub | Owns resilience; builds on the REST Client and timeouts of 07 | Minimal app with a flaky remote (stub of 07) | 🟨 |
| [24](lab-24-health-metrics/README.md) | Health and metrics | SmallRye Health (liveness vs readiness vs startup), management interface, Micrometer/Prometheus: counter, timer, gauge, common tags, cardinality | Owns health and metrics | Minimal app | 🟨 |
| [25](lab-25-logging-tracing/README.md) | Logging and tracing | `quarkus-logging-json` (text in dev and tests, JSON in the jar, flat MDC fields; console and file handler are separate options), a correlation id (`X-Request-Id`: a safe one is kept, any other replaced, MDC, response header, forwarded to the next service by a client filter), the MDC across threads (empty in `CompletableFuture`, kept by `ManagedExecutor`), OpenTelemetry tracing (server and client spans, `traceparent`, `@WithSpan`, `service.name`, trace ids in the log lines, sampling: a trace id in a log does not prove the trace exists), logs that tell the truth (no email through a record's `toString`, mapped failures are silent, levels), the jar with and without a collector | Owns logging and tracing (the request id of 04 is the starting idea) | Minimal app with a fake stock service started over HTTP (the idea of 07 and 23) | 🟨 |
| [P2](project-2-payments-gateway/README.md) | **Project 2 - a payments gateway (showcase)** | A finished service that charges cards through an external provider: JWT roles (`customer`, `finance`) and ownership, an `Idempotency-Key` that makes retries safe (replay, reuse, ten simultaneous duplicates charge once), a failure classified by "can the money have moved?" (`CAPTURED`, `DECLINED`, `FAILED`, `UNKNOWN` as a 202, or never sent and forgotten), retry, timeout and a circuit breaker around charges only, reconcile and refund, readiness that stays up on purpose, outcome counters and a gauge of payments waiting for a human, JSON logs and traces with one span per attempt, secrets that fail at startup. 154 tests against a fake provider that is idempotent like a real one, a jar run by hand with `tools/`, and a guided review with 21 break-it-yourself experiments | Uses blocks 6-7 (in memory: no data layer) | Built from scratch by the agent (no starter) | 🟨 |

## Block 8 - Reactive and communication
| Lab | Topic | What is practiced | Owns / builds on | Starts from | Status |
|-----|-------|-------------------|------------------|-------------|--------|
| [26](lab-26-execution-model/README.md) | Execution model | Where an endpoint runs (return type, `@Blocking`, `@NonBlocking`, `@RunOnVirtualThread`), a blocked event loop (the `ping` that waits, the blocked-thread warning, `await()` refused on the loop), many waits on four workers vs virtual threads, pinning of `synchronized` on Java 21 (a per-SKU lock that still defeats them), and a profile that makes the machine small and equal everywhere | Owns the execution model; `Uni` appears only as a return type (27 owns Mutiny) | Minimal app with a slow blocking driver | 🟨 |
| [27](lab-27-mutiny/README.md) | Mutiny | A `Uni` is lazy and cold (an eager `item(value)`, a `Uni` nobody subscribes to, `memoize`), composition that runs together (`Uni.join`/`combine` against `chain`), failures as an ordered list of operators (retry with back-off, the timeout before or after the retry, `recoverWithNull` and what it hides), bounded concurrency with `merge(n)`, and backpressure (`BackPressureFailure`, `onOverflow().drop()`); pipelines tested with plain JUnit, sources that count their subscriptions, `await().atMost` and `AssertSubscriber` | Owns Mutiny | Minimal app with simulated suppliers (no database, no Docker) | 🟨 |
| [28](lab-28-reactive-messaging/README.md) | Reactive messaging (in memory) | An order desk with in-app channels (`@Incoming`, `@Outgoing`, a processor, `Emitter`): the sender learns the outcome from the ack or nack (`201`, `422`, `409`, `502`, `504`), a payload method versus a `Message` method when something throws (the second cancels the whole chain for good; `PRE_PROCESSING` acks a failure), failures classified (invalid, out of stock, temporary with a retry and a dead-letter channel), where a consumer runs (event loop unless `@Blocking`), a bounded `@OnOverflow` buffer answered as `503` (`DROP` leaves the stage pending, `FAIL` kills the channel), and a misspelled channel name that only logs a warning; the packaged jar with sample orders | Owns messaging concepts (lab 29 adds the broker) | Minimal app with a slow blocking warehouse (no broker, no Docker) | 🟨 |
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
| **P3** | **Capstone project** | A complete service in three milestones: (1) API, persistence, migrations; (2) security, resilience, observability; (3) container, CI and a production checklist. A finished showcase with a guided review | Uses everything | Built from scratch by the agent | ⬜ |

## Notes for the next labs
- **Block 5 (testing) is fully built: labs 16-18 and P1.** P1 is a finished showcase (an issue tracker in `project-1-issue-tracker/app`) with `README.md` (specification, how to run) and `REVIEW.md` (guided review). Lab 18 introduced a multi-module layout (`inventory` + `orders` + `contracts/`) and a `verify.ps1` that runs `verify -DskipITs=false` when a variant has `*IT.java` and collects reports from every module (`surefire-reports` and `failsafe-reports`); `verify.ps1` also accepts `project-*` folders, whose single `app` must be green.
- **Block 6 (security) is fully built: labs 19-22.** Lab 20 keeps the token tests independent of any provider (`Tokens` helper + a throw-away key pair, labelled as such). Lab 21 (OIDC/Keycloak) should reuse the same endpoints idea, explain what changes (the provider mints the tokens, keys come from a JWKS URL, roles live in `realm_access`) and point back to the lab 20 stretch goal.
- **Block 7 (resilience and observability) is fully built: labs 23-25 and P2.** P2 (`project-2-payments-gateway`) is a finished showcase with `README.md` (specification, a guided tour of the packaged jar), `REVIEW.md` (guided review) and `tools/` (a fake provider, a token minter and sample payloads that need only the JDK). Lab 25 ships a `tools/` folder (a fake stock service that runs with `java tools/StockStub.java`, two sample orders) to run the packaged jar by hand, and test helpers worth reusing in P2: `Logs` (reads the JSON log file the tests switch on, with a mark and a polling `await`), `Stub` and `StockStub` (a fake remote with `/__control/*` endpoints, as in lab 23), an in-memory span exporter bean, and `LogProfile`/`SamplingProfile`. What P2 reuses from this block: the fake remote over HTTP with `/__control/*` endpoints (23, 25), `Ops` for the management port (24) and `Logs` plus an in-memory span exporter (25), each adapted. It adds a fake that is idempotent like a real provider and can close its port in the middle of a call, which is what lets a test count how many times money moved.
- **Block 8 (reactive and communication) has labs 26-28 built; 29 (Kafka) and 30 (gRPC) are electives still pending.** Lab 28 ships `samples/` (four order payloads for `curl.exe`), a test suite that shares one application (the test classes are ordered by task with `junit-platform.properties`, because a cancelled channel stays cancelled) and a warehouse with test hooks (`failNext`, `setDelayMillis`, `calls`). Lab 29 should reuse the order desk idea only if it helps; the lesson to carry over is the ack/nack and dead-letter vocabulary, and that `failure-strategy` of a Kafka channel decides what a nack does.
- **Projects (P1, P2, P3) are showcases, not exercises** (decided with the learner): the agent builds the whole service, documents the decisions and the traps, and the learner reviews it with the guide. Each project picks a new small domain (P1 issue tracker; P2 is the payments gateway; P3 a capstone in three milestones). Tests exist to prove the showcase works, they are not hidden acceptance tests.
- **Cross references inside READMEs** (for example "lab 26" in lab 01) must follow this numbering; `grep` for `lab [0-9]` when the numbering changes.

## Pace
One lab at a time: it is built and verified; the learner solves and reviews it whenever they can; feedback is applied to everything built so far and to what comes next.
