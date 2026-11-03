# 🛡️ Lab 23 - Fault tolerance

| ⏱️ Duration | 🚀 Quarkus | ☕ Java | 🧪 Type |
|---|---|---|---|
| 1-2 h | 3.40.1.1 LTS | 21 | A (starter + solution) |

## 📍 Where to run commands
Open a terminal **inside this lab's `starter/` folder** and check where you are before running anything:

```powershell
Get-Location
```

```bash
pwd
```

> [!NOTE]
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead. No Docker is needed: the remote service is a stub that the tests start.

## 🎯 Why it matters
Every service your code calls will, sooner or later, be slow, fail, or fail for an hour. Without a plan, its problem becomes yours: threads wait on it, the queue grows, callers retry, and a dependency that was only *slow* takes you down. Lab 07 gave the client its timeouts and error mapping. This lab adds the policies a team puts around a remote call so that a bad dependency costs you a degraded answer instead of an outage: **timeout, retry, fallback, circuit breaker and bulkhead**.

## 🧠 Concepts
| Policy | Question it answers | Typical mistake |
|---|---|---|
| **Timeout** | How long am I willing to wait? | Relying on the client default, or a timeout longer than the caller's own |
| **Retry** | Is this failure probably transient? | Retrying errors that cannot be fixed by asking again, or retrying without a pause (a retry storm) |
| **Fallback** | What can I honestly answer when I cannot get the real thing? | Inventing data, or hiding the caller's mistakes behind a fallback |
| **Circuit breaker** | Has this dependency failed enough that I should stop asking for a while? | Counting the caller's mistakes as dependency failures |
| **Bulkhead** | How many calls may be in flight at the same time? | No limit: one slow dependency uses every thread |

- In Quarkus these come from **SmallRye Fault Tolerance** (the MicroProfile Fault Tolerance annotations plus extras). They are CDI interceptors: they work on a method of a bean that another bean calls (not on a call from the same class).
- When several annotations sit on one method they wrap each other. This lab shows the order that matters most by counting real calls (task 4).
- Values in annotations are defaults; **configuration overrides them** per method (`quarkus.fault-tolerance."<class>/<method>".<policy>.<setting>`), so operations can tune a policy without a rebuild.

## 📚 Documentation
- [SmallRye Fault Tolerance](https://quarkus.io/guides/smallrye-fault-tolerance)
- Lab 07 for the REST Client, its timeouts and its error mapping.

## 🧭 Scenario
`GET /api/shipping/{city}` asks a remote **carrier** for the price (`GET /quote/{city}`). The carrier is a stub in the tests (`CarrierStub`) that the tests script: fail the next N calls with a given status, answer after a delay, count the calls and the maximum concurrency.

| Situation | What our API must do |
|---|---|
| Carrier healthy | 200, `source: "carrier"` |
| Carrier slower than 500 ms | Give up at once. Never wait for it |
| Carrier fails once or twice | The caller does not notice: 200 from the carrier |
| Carrier keeps failing | The last price we knew, `source: "cache"`. If we never knew one: **503**, `SHIPPING_UNAVAILABLE`. Never invent a price |
| City does not exist (carrier says 404) | 404, `UNKNOWN_CITY`. Not retried, not answered from the cache, **not** counted as a carrier failure |
| Carrier failing for long | Stop calling it for a while (10 s in production), then try again |
| Too many requests at once | At most **2** inside at the same time; the rest are turned away immediately (and answered from the cache) |

Already written: the client with its error mapping (`CarrierClient`), the exceptions, the endpoint, `ErrorMappers`, the price cache inside `ShippingService`. **Your job is in `ShippingService.quote` and in `application.properties`.**

## ✅ Tasks

### Task 1 - Timeout
**Goal:** a slow carrier becomes a fast answer. **Validated by:** `TimeoutTest`.

- [ ] Put a 500 ms timeout on `quote` (the client's own read timeout is 5 s, on purpose: the service policy must be the one that acts).
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=TimeoutTest'
```

```bash
./mvnw test -Dtest=TimeoutTest
```

🔮 **Predict first:** the carrier answers after 3 seconds and you have **no** `@Timeout`. How long does your caller wait, and what does it get? And with the 500 ms timeout?

### Task 2 - Retry
**Goal:** transient failures are absorbed; failures that asking again cannot fix are not retried. **Validated by:** `RetryTest`.

- [ ] Retry at most 2 times, 100 ms apart, without jitter (a test needs predictable counts; in production keep some jitter).
- [ ] Retry only when the carrier failed (`CarrierUnavailableException`) or the timeout fired. Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=RetryTest'
```

```bash
./mvnw test -Dtest=RetryTest
```

> [!WARNING]
> First write `@Retry` with only the limits and look at what `anUnknownCityIsNotRetried` says. Then restrict what is retried.

🔮 **Predict first:** with a plain `@Retry(maxRetries = 2)`, how many times does the carrier receive the request for a city that does not exist?

### Task 3 - Fallback
**Goal:** when everything else failed, answer with the last known price, or fail honestly. **Validated by:** `FallbackTest`.

- [ ] Add `@Fallback` pointing at a method `fromCache(String city)` that you write (same parameters, same return type). With no previous price it throws `ShippingUnavailableException`.
- [ ] The fallback must **not** apply to `UnknownCityException`.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=FallbackTest'
```

```bash
./mvnw test -Dtest=FallbackTest
```

🔮 **Predict first:** the cache holds Lima. A caller asks for `Atlantis`, which does not exist. With a fallback that applies to everything, what does the caller get: 404, 503 or Lima's price? Why is the answer different if the cache already held Atlantis?

### Task 4 - Circuit breaker
**Goal:** stop asking a carrier that keeps failing, ask again later, and do not blame it for the caller's mistakes. **Validated by:** `CircuitBreakerTest`.

- [ ] Open the circuit when at least 50% of the last 4 calls failed; keep it open 10 seconds; close it after 1 successful trial. Name it `ShippingService.BREAKER`.
- [ ] Only the carrier's failures (`CarrierUnavailableException`, the timeout) count.
- [ ] In `application.properties`, override the delay for the **test profile only** so that the test does not wait 10 s.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=CircuitBreakerTest'
```

```bash
./mvnw test -Dtest=CircuitBreakerTest
```

> [!WARNING]
> The unit of the delay in the annotation is seconds. What does your override mean if you write `1000`?

🔮 **Predict first:** the circuit is closed and clean; the carrier now fails every call; `@Retry` allows 2 retries. After **one** request (one attempt plus two retries), is the circuit open? After the second request, how many times in total has the carrier been called?

### Task 5 - Bulkhead
**Goal:** at most two requests at the same time reach the carrier. **Validated by:** `BulkheadTest`.

- [ ] Add the limit and run the test class.

```powershell
.\mvnw.cmd test '-Dtest=BulkheadTest'
```

```bash
./mvnw test -Dtest=BulkheadTest
```

🔮 **Predict first:** six clients call at the same moment, the carrier takes 400 ms, the limit is 2. How many are served by the carrier and what happens to the others?

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
Run the finished application as a jar and give it a real (tiny) carrier. In `solution/`:

```powershell
.\mvnw.cmd package '-DskipTests'
java -jar target\quarkus-app\quarkus-run.jar
curl.exe -s -w " [HTTP %{http_code}]`n" http://localhost:8080/api/shipping/Lima
```

```bash
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
curl -s -w " [HTTP %{http_code}]\n" http://localhost:8080/api/shipping/Lima
```

There is no carrier on `localhost:9090` yet (the configured URL), so:

```text
{"code":"SHIPPING_UNAVAILABLE","message":"The carrier is not available and there is no previous price for Lima"} [HTTP 503]
```

Start any server on port 9090 that answers `{"city":"Lima","price":12.5}` to `GET /quote/Lima`, repeat the call and get `"source":"carrier"`. Stop that server and repeat: `"source":"cache"` and the call returns in a few milliseconds. `Atlantis` (a 404 from the carrier) gives `UNKNOWN_CITY` in both situations.

## 💼 In real projects
- **A retry multiplies load.** If a carrier is slow because it is overloaded, three attempts from every caller make it worse. Use few retries, a pause with jitter (and backoff for longer chains), and the circuit breaker so retries stop when the dependency is clearly down.
- **Retry only what is safe to repeat.** A `GET` of a price: yes. A payment or an order creation: only if the operation is idempotent (an idempotency key). A retry of a request that did arrive can do the work twice.
- **Timeouts are a budget.** The caller of your API has its own timeout. If yours add up to more than theirs (attempts x timeout + pauses), you are working for nobody. In this lab: 3 x 500 ms + 2 x 100 ms is under 2 s.
- **A fallback is a product decision, not a technical one.** A stale price may be fine for a shipping estimate and a disaster for a payment. Say in the response that it is stale (`source: cache`), and never fall back to invented data.
- **Do not hide the caller's mistakes.** A 404 or a validation error is not the dependency's failure: it is not retried, does not open the circuit and is not answered by a fallback.
- **One breaker per dependency**, not one for the whole application: a failing carrier must not stop calls to a healthy one. Name them and expose their state in the health checks and metrics (lab 24).
- **Bulkhead sizes come from capacity**, not from guesses: how many concurrent calls can the dependency take, and how many threads can you spare for it?
- **Tune with configuration.** The annotation holds the safe default, the environment holds the real number (`quarkus.fault-tolerance."class/method".*`), and a test profile holds the short one.
- **These policies protect you; they do not fix the dependency.** Alerts on breaker openings and fallback rates are what tell a human that something is wrong.

## 🚀 Stretch goal
Add exponential backoff to the retry (look at `@ExponentialBackoff` in `io.smallrye.faulttolerance.api`) and jitter back, and write a test that proves the pauses grow. Then explain why this test is harder to keep stable than the ones in this lab.

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

All five policies are annotations on `quote` from `org.eclipse.microprofile.faulttolerance`, plus `@CircuitBreakerName` from `io.smallrye.faulttolerance.api`. Each one has an attribute that limits *which exceptions* it reacts to; the unknown-city rule is that attribute, in three of them.
</details>

<details><summary>Hint 2 (more concrete)</summary>

`@Timeout(value = 500, unit = ChronoUnit.MILLIS)`, `@Retry(maxRetries = 2, delay = 100, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {...})`, `@Fallback(fallbackMethod = "fromCache", skipOn = ...)`, `@CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 10, delayUnit = ChronoUnit.SECONDS, successThreshold = 1, failOn = {...})`, `@Bulkhead(2)`. The override: `%test.quarkus.fault-tolerance."lab.melostudy.ShippingService/quote".circuit-breaker.delay=1`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 (SmallRye Fault Tolerance 6.11.4). "Removing X" means: in a scratch copy of the solution, remove X, run the suite, restore.

**Task 1.** Without `@Timeout` the caller waits the full 3 seconds (`3017 ms` in the test) and then gets the price: the client's 5 s read timeout never fires. With the 500 ms timeout the call is cut: the test requires the whole request (with the retries of task 2) to finish in under 2.5 s, and it does. Removing `@Timeout` turns `slowCarrierIsCutOffQuickly` red.

**Task 2.** With a plain `@Retry`, a city that does not exist is requested **3 times** (`calls` is 3, not 1): every exception is retried unless you say otherwise. With `retryOn = {CarrierUnavailableException, TimeoutException}` it is requested once. Also with a plain `@Retry`, the exception thrown when the circuit is open is retried too (5 calls instead of 4 in the breaker test). Removing `@Retry` turns 6 tests red (the retry tests and three of the breaker tests, which rely on the retries to feed the breaker).

**Task 3.** Without `skipOn = UnknownCityException.class` the caller who asks for `Atlantis` gets **503**, not 404: the fallback swallows the caller's mistake and, with nothing cached for that city, turns it into "unavailable". Three tests turn red. The answer for a city that is in the cache would be a price for a city that does not exist, which is worse.

**Task 4.**
- One request is not enough to open the circuit if the window is clean: the one request makes 3 attempts and the volume threshold is 4. **But the window also counts successful calls.** If the service had a success just before (the cache was warmed through the carrier), 1 success + 3 failures is 4 calls with 75% failures, and the circuit opens during the first request. That is why the tests call `warmCache`, which warms the cache and then resets the breaker and the counters. With a clean window: after the second request the carrier was called **4 times in total**, and from then on **no more**.
- This also shows the order of the policies: the retry is **outside** the circuit breaker, so every attempt of a retry is one failure for the breaker. The fallback is outside the retry: it answers only after the retries are exhausted (`retriesStopAfterTheLimit` sees 3 calls, then the 503 of the fallback).
- Without `failOn` (the default counts every exception), ten requests for `Atlantis` open the circuit and a request for `Quito` gets **503**: a caller who types wrong cities can take the carrier route down for everybody.
- The override works with `%test.quarkus.fault-tolerance."lab.melostudy.ShippingService/quote".circuit-breaker.delay=1`. It keeps the unit of the annotation: `delay=1000` means **1000 seconds** and the recovery test fails (`Expected: carrier, Actual: cache`). With `delay=1000` plus `delay-unit=millis` the suite is green. Without any override the breaker stays open 10 s and the same test fails.

**Task 5.** Two requests are served by the carrier (`source: carrier`), the other four are turned away immediately and, because the cache was warmed, answered with `source: cache`. The stub saw a maximum concurrency of exactly 2. Removing `@Bulkhead` lets all **6** reach the carrier.

**Jar.** Run against a real server on port 9090 (the stub of the "What you should see" section): no carrier and no cache gave 503 in about 300 ms (3 refused connections and two pauses); the carrier up gave `source: carrier`; `Atlantis` gave 404 `UNKNOWN_CITY`; with the carrier killed, `Lima` gave `source: cache` in about 30 ms and `Quito` (never seen) gave 503.
</details>

## 🎤 Self-assessment
1. A call can be retried safely only under some conditions. Which, and what would you change in this API if the call were `POST /orders`?
2. In what order do `@Fallback`, `@Retry`, `@CircuitBreaker`, `@Timeout` and `@Bulkhead` wrap each other here, and which test shows that the retry is outside the breaker? What follows for the thresholds you choose?
3. The carrier answers 404 for a city that does not exist. Name the three policies that must not treat this as a failure and what each would do wrong.
4. Your API has a client-side timeout of 2 s and the service of this lab can take about 1.7 s in the worst case. What happens when someone sets the carrier timeout to 1 s and the retries to 5?
5. Give a case where a fallback answering with stale data is acceptable and one where it is not. How would the caller know?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
