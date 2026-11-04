# 🔍 Review - clinic appointments

This is the guide for the feedback session. Nothing here is an exercise: the service is built, **117 tests are green**, and the packaged application has run against a real PostgreSQL and in a Kubernetes cluster. Your job is to read it, run it, break it and challenge it. Every claim below was run; the experiments of section 4 were run in a scratch copy, one change at a time, and the failure messages are the real ones.

## 1. The one question, and where its answer lives
> **Can two appointments overlap for the same practitioner - with time zones, daylight saving and simultaneous requests?**

No, and the answer is in **one place that nothing can go around**: two exclusion constraints in `V1__core_schema.sql`. The Java code does not check for overlaps (a "look, then insert" is a race); it only translates the database's refusal into `SLOT_TAKEN` or `PATIENT_BUSY`. The test that proves it counts rows after twelve simultaneous requests: one 201, eleven 409, one row (`ConcurrencyTest`, run 5 times in a row).

The same question decides the other parts:
- **Time** is stored as instants and shown in the clinic's zone with its offset; working hours are *local* times. The nights the clocks change are tested in plain JUnit and through the API (`SlotCalculatorTest`, `AvailabilityTest`).
- **Reminders** have their own question - *can the same patient get the same reminder twice, or none?* - and the same style of answer: an idempotency key, a provider that counts, and a claim that two instances cannot both win.

## 2. Reading order (about 90 minutes)
| # | File | What to look for |
|---|---|---|
| 1 | `app/src/main/resources/db/migration/V1__core_schema.sql` | The constraints and their comments. Why `timestamptz`, why `'[)'`, why `WHERE status = 'BOOKED'`, why two constraints (practitioner and patient) |
| 2 | `SlotCalculator.java` and `SlotCalculatorTest.java` | Time in 80 lines. Stepping in instants; the 20 and 28 |
| 3 | `BookingService.java` | The order of the checks (documented at the top), `translatingOverlaps`, `load(...).filter(caller::owns)` |
| 4 | `Constraints.java` | Where the constraint's name comes from, and why |
| 5 | `Caller.java`, the `@RolesAllowed` on the resources, `SecurityTest.java` | Roles from `groups`, records from `patient_id` / `practitioner_id`, 404 vs 403 |
| 6 | `V2__reminders.sql`, `Reminder*.java` | `ReminderStore` (transactions), `ReminderService` (the loop), `ReminderGateway` (the policies and what each failure means) |
| 7 | `ReminderResilienceTest.java` and `NotificationStubServer.java` | Assertions made **at the provider** |
| 8 | `ClinicMetrics.java`, `ReminderReadinessCheck.java`, `HealthTest`, `MetricsTest`, `TracingAndLogsTest` | Bounded tags, a readiness that stays UP, one trace per reminder |
| 9 | `application.properties` | Every property has a reason in its comment |
| 10 | `../k8s`, the generated `target/kubernetes/kubernetes.yml`, `../PRODUCTION.md` | What the extension generates, and what is still missing |

## 3. Decisions, and the lab that taught each
| Decision | Why | Lab |
|---|---|---|
| Overlaps are an exclusion constraint, not code | a check followed by an insert is a race | 10, 11 |
| The service translates by the **name** of the constraint | "some constraint failed" is not a conflict; unknown ones stay a 500 | 5, 10 |
| Instants in the database, local times for working hours, an offset required on input | a local time can be ambiguous or not exist | 14 |
| 400 for the shape, 422 for a rule of time, 409 for the state of the world, 412/428 for versions | each answers a different question for the client | 5, 15 |
| `ETag` is the version; changes need `If-Match` | lost updates | 10, 15 |
| Filters and ids as validated text | a conversion failure is a 404 otherwise | P1 |
| Another person's appointment is a **404** | the existence of a record is information | 19 |
| `@TestSecurity` for the domain tests, real signed tokens for security, never both in one test | one test depended on which won | 20 |
| The reminder is created **in the booking's transaction** | nothing to reconcile | 10, P1 |
| Claim (short transaction, `SKIP LOCKED`, lease) - call - record, each apart | never hold a connection while waiting for a remote | 13, 23 |
| Every FT policy names its exceptions | the defaults react to everything | 23 |
| One idempotency key per message **version** | a retry is not a second message; a moved appointment is a new one | P2 |
| Readiness stays UP when the provider is down | booking does not need it | 24, P2 |
| Metric tags are short fixed lists | an id in a tag is a new series per request | 24 |
| The job sets `trace_id` / `span_id` itself | see traps | 25 |
| Migrations as a Job before the rollout; the app only validates | N replicas must not migrate at once | 11, 35 |
| The pipeline is manual | a study repository should not spend minutes on every commit | 36 |

## 4. Break it yourself
Each row is one change in a **scratch copy** of `app/` (copy the folder, change one thing, run the named tests, throw the copy away). The "you get" column is a real failure, quoted from the run. If a change leaves everything green, the test does not protect that decision - and that would be a finding.

```powershell
Copy-Item -Recurse app $env:TEMP\scratch-app -Exclude target
cd $env:TEMP\scratch-app
# ...make ONE change from the table...
.\mvnw.cmd clean test "-Dtest=ConcurrencyTest,BookingTest,SchemaTest"
```

| # | The change | Run | You get | What it proves |
|---|---|---|---|---|
| M01 | `V1__core_schema.sql`: delete the `appointment_no_overlap_practitioner` constraint | `ConcurrencyTest`, `BookingTest`, `SchemaTest` | 5 of 25 red. `ConcurrencyTest.exactlyOneOfManySimultaneousRequestsGetsTheSlot`: exactly one booking ==> expected: <1> but was: <12> | The code has no overlap check at all. With the constraint gone, twelve simultaneous requests are twelve bookings. |
| M02 | same file: `'[)'` becomes `'[]'` in the practitioner constraint (the end belongs to the range) | `SchemaTest` | 1 of 10 red. `SchemaTest.anAppointmentThatStartsWhenAnotherEndsIsNotAnOverlap`: Unexpected exception thrown: org.hibernate.exception.ConstraintViolationException: JDBC exception executing SQL [ERROR: conflicting key value violates exclusion constraint "appointment_no_overlap_practitioner"... | Back-to-back appointments (one ends at 10:00, the next starts at 10:00) become an overlap. Half-open ranges are a decision, and this is its test. (Run on `SchemaTest` alone: the same change in the whole `BookingTest` class gives 14 of 23 red, most of them `Expected status code <201> but was <500>`; I did not diagnose that cascade - read the FIRST failure of a red run, not the count.) |
| M03 | same file: remove `WHERE (status = 'BOOKED')` from the practitioner constraint | `SchemaTest`, `LifecycleTest` | 2 of 19 red. `LifecycleTest.cancellingFreesTheTimeForSomeoneElse`: Expected status code <201> but was <409>. | A cancelled appointment keeps blocking its time. |
| M04 | `BookingService.book`: put `persist` outside the `try` that translates constraint violations (only `flush` inside) | `BookingTest`, `ConcurrencyTest` | 7 of 15 red. `BookingTest.aLongTreatmentConflictsWithAShortOneInsideIt`: Expected status code <409> but was <500>. | With `IDENTITY` ids `persist()` executes the INSERT, so the violation never reaches the `try`. This was my first version. |
| M05 | `Constraints.java`: read the constraint name only from Hibernate, delete the PostgreSQL driver branch | `BookingTest`, `CatalogApiTest` | 6 of 21 red. `BookingTest.aLongTreatmentConflictsWithAShortOneInsideIt`: Expected status code <409> but was <500>. | Hibernate's `getConstraintName()` is `null` for these violations (probed), so nothing is translated. |
| M06 | `SlotCalculator`: end of a block = start + the clock difference, instead of the instant of the end | `SlotCalculatorTest`, `AvailabilityTest` | 4 of 17 red. `SlotCalculatorTest.theNightThatLosesAnHourHasFiveHoursOfSlotsNotSix`: expected: <20> but was: <24> | The night the clocks go forward has 20 quarter-hour slots, not 24; the night they go back, 28. Plain arithmetic on local times gets both wrong. |
| M07 | `BookingService.load`: delete `.filter(caller::owns)` | `SecurityTest` | 6 of 15 red. `SecurityTest.aPatientReschedulesTheirOwnAppointmentOnly`: Expected status code <404> but was <200>. | Anybody can read, move or cancel anybody's appointment: the status codes change from 404 to 200, 403 and 409. |
| M08 | `BookingService.page`: list with `filter` instead of `restricted(filter)` | `SecurityTest` | 2 of 15 red. `SecurityTest.aListShowsOnlyWhatBelongsToTheCaller`: JSON path total doesn't match. Expected: <1> | A patient or a practitioner lists every appointment of the clinic. |
| M09 | `application.properties`: `deny-unannotated-endpoints=false` | `SecurityTest` | 1 of 15 red. `SecurityTest.anEndpointNobodyAnnotatedIsClosedEvenToReception`: Expected status code <403> but was <200>. | An endpoint somebody forgot to annotate is open. |
| M10 | `BookingService.book`: delete the check that a patient books only for themselves | `SecurityTest` | 2 of 15 red. `SecurityTest.aPatientBooksForThemselvesAndForNobodyElse`: Expected status code <403> but was <201>. | A patient books for anybody. |
| M11 | `ReminderGateway`: `@Retry` without `retryOn` and `abortOn` (the defaults: any exception) | `ReminderResilienceTest` | 2 of 7 red. `ReminderResilienceTest.aRefusalForGoodIsNotRetriedAndWaitsForAPerson`: a 400 is not retried ==> expected: <1> but was: <3> | A 400 is retried twice (three calls) and the six refusals of the second test make eighteen calls. |
| M12 | `ReminderGateway`: `@CircuitBreaker` without `failOn` and `skipOn` | `ReminderResilienceTest` | 1 of 7 red. `ReminderResilienceTest.aRefusalForGoodDoesNotCountAgainstTheCircuitBreaker`: every one was tried and refused ==> expected: <6> but was: <4> | Six refusals for good open the breaker: only four were refused, the other two were put off by the open breaker. One wrong address degrades everybody's reminders. |
| M13 | `ReminderGateway`: a new `UUID` as idempotency key in every HTTP call | `ReminderResilienceTest` | 1 of 7 red. `ReminderResilienceTest.anAnswerThatIsLostAfterTheProviderDeliveredDoesNotSendTheMessageTwice`: expected: <1> but was: <0> | A lost answer is retried with a different key and the patient gets TWO messages (the provider counts them). |
| M14 | `ReminderStore.claim`: the lease is `now` instead of `now + 2 minutes` | `ReminderResilienceTest` | 1 of 7 red. `ReminderResilienceTest.severalInstancesPickingAtOnceNeverSendTheSameReminderTwice`: six reminders, sent by whoever picked them ==> expected: <6> but was: <11> | Four pickers at once send reminders more than once. **Red in 4 of 4 runs** (the first and three more), because the claim is only safe with the lease. |
| M15 | `ReminderStore.current`: delete the check that the appointment is still at the sent version | `ReminderTest` | 1 of 10 red. `ReminderTest.anAnswerForAnAppointmentThatMovedInTheMeantimeIsDropped`: JSON path [0].status doesn't match. Expected: PENDING | The provider's answer for the OLD time marks the reminder as sent, and the patient is never told the new time. |
| M16 | `ReminderService`: an open breaker calls `failedTransiently` instead of `postpone` | `ReminderResilienceTest` | 1 of 7 red. `ReminderResilienceTest.anOutageOpensTheBreakerAndTheRestOfTheBatchIsPutOffWithoutCountingAnAttempt`: postponed reminders keep attempts = 0 ==> expected: <7> but was: <6> | Reminders that were put off are no longer untouched (`attempts = 0`): an outage consumes the attempts of reminders that were never even tried. |
| M17 | `ReminderReadinessCheck`: DOWN when the breaker is open | `HealthTest` | 1 of 4 red. `HealthTest.aProviderOutageDoesNotTakeTheClinicOutOfRotation`: Expected status code <200> but was <503>. | A provider outage takes every instance out of rotation: nobody can book an appointment. |
| M18 | `AppointmentRepository`: delete the three `join fetch` | `ListingTest` | 1 of 5 red. `ListingTest.aPageOfTwentyRowsCostsTwoStatementsNotTwentyThree`: statements: 5 ==> expected: <true> but was: <false> | A page of twenty appointments costs 5 statements here (the batch fetching of Hibernate hides most of the N+1; with more associations or pages it grows). |
| M19 | `ReminderService`: delete the `trace_id` line of the MDC | `TracingAndLogsTest` | 1 of 4 red. `TracingAndLogsTest.aRefusedReminderIsAnErrorSpanAndItsLogLineCarriesTheSameIds`: the log line points at the trace that exists ==> expected: <11b367d27ef3c153acafe288e64bc9d9> but was: <null> | The log line of a refused reminder no longer points at the trace that exists. |
| M20 | `BookingService.transition`: delete `reminders.cancelPending(a)` | `ReminderTest` | 1 of 10 red. `ReminderTest.cancellingTheAppointmentCancelsAReminderThatHasNotGoneOut`: JSON path [0].status doesn't match. Expected: CANCELLED | The patient is reminded of an appointment they cancelled. |
| M21 | `ReminderStore.failedTransiently`: the wait is always `backoff`, it does not double | `ReminderExhaustionTest` | 1 of 1 red. `ReminderExhaustionTest.theWaitDoublesAfterEveryFailureAndTheFifthOneGivesUp`: wait after attempt 2 ==> expected: <PT2M> but was: <PT1M> | A provider that is down is hit every minute instead of every 1, 2, 4, 8. |
| M22 | `ErrorMappers`: the rejection counter is tagged with the message, not the code | `MetricsTest` | 1 of 5 red. `MetricsTest.refusalsByTheRulesOfTheClinicAreCountedByCodeAndOtherErrorsAreNot`: expected: <1.0> but was: <0.0> | A test that asks for the series by code finds nothing - and in production every distinct message would be a new series. |

All 22 turned at least one test red. Two honest notes on how the list was made: the first run of M02 on the whole `BookingTest` class showed 500s that a single test does not reproduce (see its row), and M14 was repeated three more times because concurrency can hide: it was red every time.

## 5. Traps I fell into while building it
1. **`persist()` runs the INSERT.** With `GenerationType.IDENTITY` the row is inserted inside `persist()`, so a `try` around `flush()` sees nothing: every violation was a 500 (M04).
2. **Hibernate did not know the constraint's name.** I first blamed the exception type (`PersistenceException` against `RuntimeException`) and wrote a comment saying so. A mutation (put it back, still green) showed that was wrong. A probe showed `getConstraintName()` was `null` with SQLState `23P01`; the name now comes from the driver (`PSQLException`) (M05).
3. **Do not assume how many failures open a breaker.** A circuit breaker counts attempts, a success counts too, and the order of requests changes when it opens: the tests shrink the window by configuration and assert properties (some were put off, fewer calls than tries) instead of exact counts.
4. **A reminder must never wait for the provider inside a transaction** (a trap avoided by design, not one I fell into). The shape is claim (a short transaction, `SKIP LOCKED`, a lease), then the call with no transaction, then record; M14 shows what the lease is for.
5. **`traceId` and `spanId` are not yours.** The Quarkus OpenTelemetry integration owns those MDC keys and clears them outside a request: a job's log line never had them (the same code with `trace_id` and `span_id` did). A log line without a trace id looks fine until you need it.
6. **The first draft of the test fixtures was wrong twice:** a "now" that left 3 of 8 reminders not yet due, and `hasSize(7)` for a four-hour morning of 30-minute slots (it is 15). Compute the expected number from the fixture, not from memory.
7. **A transient failure left no log line.** Found by running the jar with the provider dead and reading its log: nothing. Now there is a WARN with the next attempt time and a test for it.
8. **The generated manifest cannot be applied twice.** The extension writes the Flyway migration as a `Job`; a finished Job is immutable, so the second `kubectl apply` of the *same* manifest fails. The deploy procedure deletes it first. The extension also generates a `ServiceMonitor`, which a cluster without the Prometheus operator refuses.
9. **PowerShell 5.1 prepends a byte-order mark to text piped into a program.** It happened twice in this milestone (a loop of 40 failed JSON parses) and it is in this repository's lessons since lab 29.
10. **Two background runs of the same experiment script fight over one folder.** The second died on the first's scratch directory; I only noticed because the first kept going.

## 6. What is deliberately missing
See [`PRODUCTION.md`](PRODUCTION.md): the signing key (this repository's private key is public), a real identity provider, backups, alerts, a registry, an audit trail, erasure of personal data, a trace collector. It is not an oversight: the point of the project is the questions above.

## 7. Questions to challenge me with
1. The availability endpoint and the booking rules are two implementations of "when can this be booked". `AvailabilityTest.everySlotOfTheResponseCanReallyBeBooked` ties them. What would you do to make them one implementation, and what would it cost?
2. A practitioner's working hours change on Monday. What happens to the appointments already booked in the hours that disappeared? Is that a bug?
3. The clinic has one time zone. A second clinic in Lima shares the service. Which table gets a column, which constant disappears, and what breaks in `SlotCalculator`?
4. `FOR UPDATE SKIP LOCKED` plus a lease makes the claim safe. What if the lease (2 minutes) is shorter than a slow provider call plus its retries? What if the pod dies after the provider delivered and before the record?
5. The key of a reminder is `reminder-{id}-v{version}`. A patient is booked, the appointment is rescheduled twice, and the second message is lost. Which messages did the provider deliver, and which one did the patient finally see?
6. Why does `GET /api/reminders` need `retry` to be a separate, explicit action instead of the job retrying `FAILED` reminders forever?
7. `readiness` is always UP. When *would* you make it DOWN for this service?
8. A 401 and a 403 are different. A 404 for somebody else's appointment hides it. What does the timing of the answer reveal (a database lookup happens in one case and not in the other)? Is that acceptable here?
9. The Flyway Job runs before every rollout. A migration that drops a column ships with the new code. The old pods are still running. What breaks, and what is the standard way to avoid it?
10. The pipeline is manual. Write the three lines of a branch protection rule you would want in a real team, and say what each would cost this repository.
