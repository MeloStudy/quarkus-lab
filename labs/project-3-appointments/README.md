# 🏥 Project 3 - Clinic appointments (capstone)

| ⏱️ Duration | 🚀 Quarkus | ☕ Java | 🧪 Type |
|---|---|---|---|
| a guided review per milestone | 3.40.1.1 LTS | 21 | C (finished showcase, built in three milestones) |

> [!NOTE]
> Like P1 and P2, this is **not an exercise**: the service is already built, with its tests green. You read it, run it, break it and challenge it in the feedback session. P3 is built in three milestones, each a commit, each verified before the next starts. The guided review (`REVIEW.md`) is written when the last one is done.

## 🎯 The one question
> **Can two appointments overlap for the same practitioner - with time zones, daylight saving and simultaneous requests?**

Everything is designed around that question: the answer lives in the **database** (exclusion constraints), time is handled as **instants** that are shown in the clinic's zone, and the tests count what actually happened (one booking out of twelve simultaneous requests) instead of trusting the answer of the API.

## 🧭 Milestones
| # | Scope | Status |
|---|---|---|
| 1 | **API, persistence, migrations**: practitioners, treatments, patients, working hours, availability, booking, rescheduling, cancelling; Flyway; one error contract; `ETag` / `If-Match`; paging | ✅ built (70 tests) |
| 2 | **Security, resilience, observability**: JWT roles and ownership, reminders through a fake notification provider (timeout, retry, circuit breaker, idempotency key), health, metrics, JSON logs, traces | ✅ built (116 tests) |
| 3 | **Container, CI, production checklist**: image, Kubernetes manifests, the manual workflow of lab 36, the packaged jar against PostgreSQL, `PRODUCTION.md`, `REVIEW.md` | ⬜ |

## ▶️ How to run it
Open a terminal **inside `app/`** (Docker must be running: PostgreSQL starts by itself, lab 8).

```powershell
.\mvnw.cmd clean verify
```

```bash
./mvnw clean verify
```

Dev mode (live reload, sample data, Swagger UI at `/q/swagger-ui`):

```powershell
.\mvnw.cmd quarkus:dev
```

## 🧱 Stack
Quarkus REST + Jackson, Hibernate ORM with Panache (repositories), Flyway, Hibernate Validator, SmallRye OpenAPI, SmallRye JWT, REST Client, Fault Tolerance, Scheduler, SmallRye Health, Micrometer (Prometheus), JSON logging, OpenTelemetry, PostgreSQL 17.11 (Dev Services, pinned). Everything is inside the Quarkus BOM. MapStruct (lab 12) is **deliberately not used**: the responses are records with a static `of(...)`, which is enough for six small shapes and one dependency less.

## 📐 Milestone 1 - the specification

### The model
| Table | What it holds | Rules the **database** enforces |
|---|---|---|
| `practitioner` | who works here | name not blank |
| `treatment` | what is done, with its length | unique name; 15 to 240 minutes, a multiple of 15 |
| `working_hours` | **local** blocks of time per weekday (1 = Monday ... 7 = Sunday) | start before end; two blocks of one day cannot overlap (exclusion constraint) |
| `patient` | who comes | unique email, case-insensitive |
| `appointment` | an interval of **instants** (`timestamptz`), a status, a version | end after start; status in a list; **no two BOOKED appointments of one practitioner overlap; none of one patient either** (exclusion constraints on `tstzrange(starts_at, ends_at, '[)')`, only for `BOOKED`) |

### The clinic
One time zone for the whole service (`clinic.time-zone`, `Europe/Madrid`), a 15-minute grid (`clinic.slot-minutes`) and a booking horizon of 90 days (`clinic.horizon-days`). They are an injected `@ConfigMapping`: a wrong zone stops the startup.

### Endpoints
| Method and path | What it does | Answers |
|---|---|---|
| `POST /api/practitioners`, `GET /api/practitioners`, `GET /api/practitioners/{id}` | the staff | 201 + `Location`, 200, 404 |
| `PUT /api/practitioners/{id}/working-hours`, `GET ...` | replaces / reads the whole week (`{"blocks":[{"weekday":1,"start":"09:00","end":"13:00"}]}`) | 200, 422 `INVALID_HOURS` / `OVERLAPPING_HOURS` |
| `GET /api/practitioners/{id}/availability?date=2026-03-09&treatmentId=1` | every start at which the treatment fits, minus bookings, the past and the horizon | 200 (empty list on a day off), 400 `INVALID_DATE` |
| `POST /api/treatments`, `GET /api/treatments` | what is done | 201, 409 `TREATMENT_EXISTS`, 422 `INVALID_DURATION` |
| `POST /api/patients`, `GET /api/patients/{id}` | who comes | 201, 409 `EMAIL_TAKEN` |
| `POST /api/appointments` | books (`practitionerId`, `patientId`, `treatmentId`, `startsAt`) | 201 + `Location` + `ETag`; 409 `SLOT_TAKEN` / `PATIENT_BUSY`; 422 time rules |
| `GET /api/appointments/{id}` | one appointment, with its `ETag` (= version) | 200, 404 |
| `GET /api/appointments?practitionerId&patientId&status&from&to&page&size` | filtered page, ordered by start; `from` and `to` are **days of the clinic, both included** | 200 `{items, page, size, total}`, 400 |
| `POST /api/appointments/{id}/reschedule` (`If-Match`) | moves a booked appointment that has not started | 200, 409, 412, 422, 428 |
| `POST /api/appointments/{id}/status` (`If-Match`) | `BOOKED` -> `CANCELLED` (before it starts), `COMPLETED` or `NO_SHOW` (once it has started) | 200, 409, 412, 428 |

An appointment looks like this: instants in UTC, plus the same start **in the clinic with its offset**:

```json
{"id":1,"practitionerId":1,"practitionerName":"Dr. Ana","patientId":1,"patientName":"Bea","treatmentId":1,"treatmentName":"Checkup",
 "startsAt":"2026-03-09T09:00:00Z","endsAt":"2026-03-09T09:30:00Z","local":"2026-03-09T10:00:00+01:00","status":"BOOKED","version":0}
```

### The rules of time (all in the clinic's zone)
1. `startsAt` **must carry an offset or `Z`**. A local time without a zone is a `400 MALFORMED_REQUEST`: it can be ambiguous (the hour that happens twice) or not exist (the hour that is skipped).
2. It must be on the 15-minute grid of the clinic's clock (`INVALID_START`), not in the past (`IN_THE_PAST`) and within 90 days (`TOO_FAR`).
3. It must fit **entirely inside one block** of that weekday's working hours (`OUTSIDE_WORKING_HOURS`): it cannot cross the lunch break or the closing time.
4. Overlaps are **not checked in the code**: a check followed by an insert is a race. The exclusion constraints decide and the service translates their verdict (`SLOT_TAKEN`, `PATIENT_BUSY`).

The order of the checks is part of the contract: **existence (404) -> version (428, 412) -> state (409) -> shape of the time (422) -> the database (409)**.

### The error contract
One shape for everything, expected or not:

```json
{"code":"SLOT_TAKEN","message":"The practitioner is already booked at that time"}
```

| Status | Codes |
|---|---|
| 400 | `VALIDATION_ERROR` (with `errors[{field,message}]`), `MALFORMED_REQUEST`, `INVALID_DATE` |
| 404 | `NOT_FOUND` |
| 409 | `SLOT_TAKEN`, `PATIENT_BUSY`, `EMAIL_TAKEN`, `TREATMENT_EXISTS`, `INVALID_TRANSITION`, `ALREADY_STARTED`, `TOO_EARLY` |
| 412, 428 | `PRECONDITION_FAILED`, `PRECONDITION_REQUIRED` |
| 422 | `INVALID_START`, `IN_THE_PAST`, `TOO_FAR`, `OUTSIDE_WORKING_HOURS`, `INVALID_HOURS`, `OVERLAPPING_HOURS`, `INVALID_DURATION` |
| 500 | `INTERNAL_ERROR` with an `errorId` that is also in the log, and nothing else |

### What the tests prove (70)
| Class | What it protects |
|---|---|
| `SlotCalculatorTest` (plain JUnit) | the arithmetic of time: the night the clocks go forward has **20** quarter-hour slots between 00:00 and 06:00, not 24; the night they go back has **28**, and `02:00` happens twice |
| `SchemaTest` | each constraint, by name, with raw SQL and no API: overlaps, back-to-back, cancelled rows, patients, checks, working hours, emails |
| `AvailabilityTest` | the endpoint in the clinic's zone, bookings removed, the daylight-saving days **through the API**, and that **every offered slot can be booked** |
| `BookingTest` | each rule of time with a valid request as baseline, the order of the checks, a refused booking leaves nothing |
| `LifecycleTest` | the state machine, `ETag` / `If-Match`, the clock |
| `ConcurrencyTest` | **twelve simultaneous requests for one slot: exactly one 201 and eleven 409**; one patient asking two practitioners at once |
| `ListingTest` | filters, days of the clinic, paging, and **two SQL statements for a page of twenty** |
| `ErrorContractTest`, `CatalogApiTest` | the one error shape and the reference data |

Time in the tests is a fixed `Clock` (`Clinic.NOW` is Monday 2026-03-02 09:00 in Madrid); the clocks go forward on 2026-03-29 and back on 2026-10-25.

## ✅ Verified during the build
- `ConcurrencyTest` ran **5 times in a row** without a failure.
- Dev mode starts with the sample data and serves the API and Swagger UI.
- A mutation experiment showed that the translation of the database's verdict needed **two** things I had not expected (they are in `REVIEW.md` when it is written): the `try` must cover `persist()` and not only `flush()`, and Hibernate's `getConstraintName()` is empty for these violations - the name comes from the PostgreSQL driver.

## 📐 Milestone 2 - security, resilience, observability

### Who may do what
Tokens are JWTs verified with a public key (issuer `https://clinic.example/issuer`, audience `clinic-api`). The roles are the `groups` claim; a patient token also carries `patient_id` and a practitioner token `practitioner_id` (numbers). **A role without its claim is a token the clinic cannot place: it sees and changes nothing that belongs to somebody.** Every endpoint is annotated and the default is closed (`deny-unannotated-endpoints`).

| | `reception` | `practitioner` | `patient` |
|---|---|---|---|
| Read the catalog (practitioners, treatments, working hours) and the availability | yes | yes | yes |
| Create practitioners, treatments, patients; replace working hours | **yes** | no (403) | no (403) |
| Read a patient record | any | no (403) | **only their own** (anybody else: 404) |
| Book | for anybody | no (403) | **only for themselves** (403 for another `patientId`, even one that does not exist) |
| Read / list appointments | all | **their own** | **their own** |
| Reschedule | any | no (403) | their own |
| Cancel | any | their own | their own |
| Complete or mark no-show | any | **their own** | no (403) |
| Reminders (`/api/reminders`) | **yes** | no (403) | no (403) |

Somebody else's appointment is a **404**, exactly like one that does not exist: its existence is not revealed. A list asked for somebody else's records (`?patientId=` of another patient) is a 403, not silently filtered. No token, or one that cannot be trusted (bad signature, expired, wrong issuer, wrong audience): 401.

### Reminders
Booking creates a `reminder` row **in the same transaction** (no outbox to reconcile: both exist or neither). It is due 24 hours before the appointment, or at once if the appointment is closer. Rescheduling makes it pending again for the new time (a new idempotency key, because the key carries the appointment's version); cancelling, completing or missing cancels it unless it already went out.

A job (`@Scheduled`, every 30 s, `SKIP` if the previous tick is still running) does three things, **each in its own transaction, none while calling the provider**:
1. **Claim**: `UPDATE ... WHERE id IN (SELECT ... FOR UPDATE SKIP LOCKED LIMIT n) RETURNING id` pushes `next_attempt_at` two minutes ahead. Two instances never pick the same row, and a crash only delays a reminder.
2. **Send** through `ReminderGateway` (REST client + timeout 2 s + retry 2 times + circuit breaker, every policy naming its exceptions).
3. **Record** the answer, but only if the reminder is still pending *and* the appointment is still at the version that was sent; an answer for a time that moved meanwhile is dropped.

How a failure is classified (the question is "will asking again help?"):

| What happened | Meaning | What the clinic does |
|---|---|---|
| 2xx | accepted | `SENT` |
| 4xx | the provider says *never* (bad address) | `FAILED` at once, no retry, **does not count against the breaker**; a person fixes it and calls `POST /api/reminders/{id}/retry` |
| 5xx, 429, timeout, connection refused or dropped | *not now* | retried twice at once with the **same idempotency key**; if still failing, one attempt is counted and the next one waits twice as long (1 min, 2, 4, 8); the fifth gives up: `FAILED` |
| the breaker is open | the provider is known to be down | the rest of the batch is **put off for 30 s without counting an attempt**: it is not the reminder's fault |
| a lost answer after the provider delivered | looks like a failure, was a success | the retry carries the same key and the provider recognizes it: one message |

### Operations
- **Management port** (9000; 9001 in tests): `/q/health/live`, `/q/health/ready`, `/q/metrics`. None of them is on the public port.
- **Readiness stays UP when the reminders are in trouble**, on purpose: booking does not need the provider. The `reminders` check reports `circuit`, `pending`, `failed` and `oldestOverdueSeconds` as data for the operator.
- **Metrics** (tags are short fixed lists, never ids): `clinic_appointments_total{outcome}`, `clinic_requests_rejected_total{code}` (only 409 and 422), `clinic_reminders_total{result}`, `clinic_notifications_call_seconds{outcome}`, and the gauges `clinic_reminders_pending`, `clinic_reminders_failed` ("a person must look") and `clinic_reminders_overdue_seconds`.
- **Logs** are JSON in production with the MDC as top-level fields: `reminderId`, `appointmentId`, `trace_id`, `span_id`. No personal data (no emails) in a log line.
- **Traces**: one `reminder.send` span per reminder; the HTTP call to the provider is its child and the provider receives the trace id (`traceparent`).

### Configuration that matters
| Property | Meaning | Missing |
|---|---|---|
| `mp.jwt.verify.publickey.location`, `mp.jwt.verify.issuer`, `mp.jwt.verify.audiences` | how tokens are verified | the defaults in `application.properties` point at the public key inside the jar |
| `quarkus.rest-client.notifications.url`, `notifications.api-key` | the provider | **no default for the key: the application refuses to start** (validated `@ConfigMapping`) |
| `clinic.reminders.*` | lead time, batch size, attempts, backoff, lease, pause when the provider is down, poll interval | defaults in `ReminderConfig` |
| `clinic.time-zone`, `clinic.slot-minutes`, `clinic.horizon-days` | the clinic | defaults, zone validated at startup |

### What the new tests prove (116 in all)
| Class | What it protects |
|---|---|
| `SecurityTest` (15, real tokens, no `@TestSecurity`) | 401 for each way a token can be untrustworthy, a closed default, each role rule with its allowed twin, somebody else's appointment is a 404 identical to a missing one, lists scoped to the caller, a token with a role and no record claim sees nothing |
| `ReminderTest` (10) | created with the booking, due 24 h before or at once, sent exactly once, cancelled/moved/stale answers |
| `ReminderResilienceTest` (7) | temporary failure retried **with the same key and delivered once**; a lost answer delivers **once**; a 400 is not retried and does not open the breaker; an outage opens the breaker and puts the rest of the batch off without counting attempts; recovery delivers everybody once; **four simultaneous runs pick each reminder exactly once** - all counted at the fake provider |
| `ReminderExhaustionTest` | the wait doubles (60, 120, 240, 480 s) and the fifth failure gives up |
| `ReminderSchedulerTest` | nobody calls the job: the scheduler sends it |
| `HealthTest`, `MetricsTest`, `TracingAndLogsTest` | readiness stays UP with the provider down; counters, gauges and tags; one trace per reminder, the provider receives its id, the log line carries the same ids and no email |

## ✅ Verified during milestone 2
- `SecurityTest` and `ReminderResilienceTest` ran **5 times in a row** without a failure; the operations classes 3 times.
- The fake provider is idempotent like a real one, so "how many messages went out" is read **at the provider**, not from the application's own report.
- One finding that goes into `REVIEW.md`: the Quarkus OpenTelemetry integration owns the MDC keys `traceId` and `spanId` and they never reached the log line written by the reminder job (measured: the same code with `trace_id` and `span_id` did), so the job sets its own keys.
