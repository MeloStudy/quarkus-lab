# ⏱️ Lab 13 - Cache and scheduled tasks

| ⏱️ Duration | 🚀 Quarkus | ☕ Java | 🧪 Type |
|---|---|---|---|
| 2 h | 3.40.1.1 LTS | 21 | A (starter + solution) |

## 📍 Where to run commands
Open a terminal **inside this lab's `starter/` folder** and check where you are before running anything:

```powershell
Get-Location
```

```bash
pwd
```

> [!NOTE]
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead.

> [!NOTE]
> No Docker needed: this lab has no database. The "slow remote system" is simulated by a class that sleeps.

## 🎯 Why it matters
A cache is the cheapest performance fix there is, and the one that most often returns wrong data in production: nobody notices until a customer sees yesterday's price. A scheduled job is the cheapest way to do periodic work, and the one that most often runs twice, or on top of itself. Both are a single annotation, and both hide a design decision behind it: *when is the data allowed to be old?* and *what if the previous run is still going?*

## 🧠 Concepts
- **`@CacheResult(cacheName = "...")`:** the first call with a given key runs the method and stores the result; later calls with the same key return the stored value without running it. The **key** is built from the method parameters.
- **`@CacheKey`:** marks which parameters form the key when not all of them should. With no `@CacheKey`, *all* parameters are the key.
- **`@CacheInvalidate` / `@CacheInvalidateAll`:** remove one entry (computed from the key, like above) or all of them.
- **Expiry and size** are configuration of the Caffeine cache: `quarkus.cache.caffeine."name".expire-after-write`, `.maximum-size`. Expiry is your safety net for every change nobody tells the cache about.
- **`@Scheduled`:** `every = "10m"` (an interval) or `cron = "..."`. Both accept a property expression such as `"{rates.refresh.every}"`, and the value `off` disables the job.
- **`concurrentExecution`:** `PROCEED` (default) lets runs overlap; `SKIP` drops a fire time while the previous run is still going. It only knows about **this** application instance.

| Question | Where it is decided |
|---|---|
| What is the key? | Method parameters, narrowed with `@CacheKey` |
| When is an entry removed? | `@CacheInvalidate*`, expiry, size limit |
| What if upstream changes behind our back? | Only expiry helps |
| What if a job outlives its interval? | `concurrentExecution` |
| What if two instances run the same job? | Not solved by this extension (see "In real projects") |

## 📚 Documentation
- [Quarkus - Application data caching](https://quarkus.io/guides/cache)
- [Quarkus - Scheduling periodic tasks](https://quarkus.io/guides/scheduler)
- [Quarkus - Scheduler reference](https://quarkus.io/guides/scheduler-reference)

## 🧭 Scenario
`starter/` is a tiny exchange-rate service: `GET /rates/{currency}`, `GET /rates/{currency}/quote?amount=n` and `PUT /rates/{currency}`. Every read of the upstream (`RateProvider`) takes 150 ms and is counted, so the tests can tell if an answer came from upstream or from the cache. `RateRefresher` is a periodic job that already exists; it works, but not well enough.

Everything you write is in `RateService`, `RateRefresher` and `application.properties`. There are no cache annotations anywhere yet. The cache must be called `rates` (the tests clear it by that name).

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

## ✅ Tasks

### Task 1 - Cache the reads
**Goal:** repeated reads of a currency pay the upstream latency once. **Validated by:** `CacheTest`.

- [ ] Annotate `rateOf` so its results are cached in a cache named `rates`.
- [ ] Configure the cache in `application.properties`: at most 100 entries, each one valid for 5 minutes, and a test-profile override of **1 second** (task 2 needs it).
- [ ] Look at `quote`: it calls `rateOf` on `this`.

🔮 **Predict first:** (a) three `quote` calls for the same currency: how many upstream calls? (b) ten requests for the same, not yet cached, currency arrive at the same moment: how many upstream calls? (c) a request for an unknown currency fails twice: how many upstream calls?

```powershell
.\mvnw.cmd test "-Dtest=CacheTest"
```

### Task 2 - Never serve data that is wrong for long
**Goal:** a changed rate is visible at once, other rates stay cached, and changes nobody tells us about disappear on their own. **Validated by:** `InvalidationTest`.

- [ ] `change` writes upstream. Make it remove **only that currency** from the cache.
- [ ] `forgetAll` must empty the whole cache.

> [!WARNING]
> Invalidation can be wrong without any error. Write the obvious version first (the annotation on `change`) and read what the tests say. Then think about what the cache uses as the key.

🔮 **Predict first:** `provider.update(...)` is called directly (a nightly import, another service, a manual fix), bypassing `RateService`. How long does a client keep seeing the old rate? What decides that number?

```powershell
.\mvnw.cmd test "-Dtest=InvalidationTest"
```

### Task 3 - A job that takes longer than its interval
**Goal:** a run never overlaps the previous one. **Validated by:** `SchedulerTest`.

`RateRefresher.refresh` "pulls fresh rates" (it sleeps) and then empties the cache. The test makes it fire every second while the work takes 1.5 seconds.

- [ ] Run the test as it is and read the failure.
- [ ] Change the job so overlapping runs cannot happen. Read the other values of the same annotation attribute and decide why you chose yours.
- [ ] Find out how the interval is read from configuration and why the other tests are not disturbed by the job (hint: the test profile).

> [!WARNING]
> The scheduler is not a stopwatch. Try an interval of 100 ms in the test profile and watch what the test reports (and what the documentation says about sub-second intervals) before trusting a short interval.

```powershell
.\mvnw.cmd test "-Dtest=SchedulerTest"
```

### Task 4 - Outside the tests: the packaged jar
**Goal:** see the cache work in a real process. No test validates this task.

```powershell
.\mvnw.cmd package
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

- [ ] In another terminal, call the same currency three times and compare the times:

```powershell
1..3 | ForEach-Object { curl.exe -s -o NUL -w "%{http_code} %{time_total}s`n" "http://localhost:8080/rates/USD" }
```

```bash
for i in 1 2 3; do curl -s -o /dev/null -w "%{http_code} %{time_total}s\n" http://localhost:8080/rates/USD; done
```

- [ ] Change the rate through the API and read it back; then read an unknown currency:

```powershell
curl.exe -s -X PUT -H "Content-Type: application/json" -d '{\"rate\":2.5}' "http://localhost:8080/rates/USD"
curl.exe -s "http://localhost:8080/rates/USD"
curl.exe -i "http://localhost:8080/rates/ZZZ"
```

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
In the jar, the first call is slow (the upstream latency plus the cold start of the process) and the next ones take a few milliseconds:

```text
200 0.360125s
200 0.003598s
200 0.002975s
```

After the `PUT`, the next `GET` already shows the new rate (`{"currency":"USD","rate":2.5}`), and an unknown currency answers `404`.

## 💼 In real projects
- **Decide the acceptable staleness first, then the TTL.** "Prices may be 5 minutes old" is a business statement; `expire-after-write=5M` is its implementation. Invalidation on writes is an optimization on top of the TTL, never a replacement: every write path you forget (a batch job, another service, a hand-run SQL) is stale data until the TTL.
- **Cache keys are part of the design.** Anything that changes the answer must be in the key (user, locale, tenant). Caching a per-user answer under a global key shows one user's data to another one; this is a security bug, not a performance one.
- **Cache immutable values** (records, `BigDecimal`, unmodifiable collections). A cached mutable object is shared by every caller; one of them changing it changes it for all.
- **A cached `null` is a cached answer.** "Not found" stays "not found" until expiry even if the thing is created a second later. Exceptions are not cached, as you saw.
- **Local caches are per instance.** With three instances, each has its own copy: a `PUT` that reaches one instance does not invalidate the others. Short TTLs, or a distributed cache (Redis, an invalidation message) when that matters.
- **Cache memory is heap.** Always set `maximum-size`, or an unbounded key space (user ids, search texts) becomes an `OutOfMemoryError` months later. Add `metrics-enabled` and watch the hit ratio: a cache with a low hit ratio is only overhead.
- **Cache a slow, stable, shared, frequently-read thing.** Measure first. A cache in front of a fast query adds a failure mode and no benefit.
- **Scheduled jobs run in every instance.** Two instances means two runs at the same time; `concurrentExecution = SKIP` does not see the other instance. If a job must run once per cluster, you need a lock in the database, the Quartz extension in clustered mode, or an external scheduler (a Kubernetes `CronJob`) calling an endpoint or running a separate process.
- **Make jobs idempotent** and log their start, duration and outcome. A job that failed halfway and runs again must not double-charge anyone.
- **Do not hide business-critical work in a timer.** If it must happen, keep a record of what is pending (a table, a queue) and let the job only process it.
- **Scheduled methods run on worker threads**, so blocking is allowed, but a job that blocks for minutes holds a thread from the pool.

## 🚀 Stretch goal
Add a second cache, `quotes`, keyed by currency **and** amount, and make `change` invalidate every quote of that currency. `@CacheInvalidate` removes exactly one key, so this needs another tool (look at the `CacheManager` API and at Caffeine's own interface). Then decide whether it is worth the complexity compared with leaving `quote` uncached. No hints.

## 💡 Hints
<details><summary>Hint 1 (direction, task 1)</summary>

The annotations live in `io.quarkus.cache`. Configuration keys start with `quarkus.cache.caffeine."rates"`. For a different value in tests, use the `%test.` prefix.
</details>

<details><summary>Hint 2 (task 2)</summary>

`@CacheInvalidate(cacheName = "rates")` builds its key from the method parameters, exactly like `@CacheResult`. Compare the parameters of `rateOf` and of `change`.
</details>

<details><summary>Hint 3 (task 2)</summary>

If every other currency is also thrown away after a `PUT`, you used the right idea on the wrong scope. The annotation for "everything" is a different one.
</details>

<details><summary>Hint 4 (task 3)</summary>

Look at the values of `Scheduled.ConcurrentExecution` (`javap` on the `quarkus-scheduler-api` jar, or the IDE).
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1.

**Task 1.**
- (a) **One** upstream call. The call of `rateOf` from `quote` is made on `this` and it is still served from the cache: Quarkus intercepts it, so moving `quote` to another bean is not needed.
- (b) **One** upstream call: while the first request computes the value, the other nine wait for it. This is the *cache stampede* protection, and it works inside one instance only.
- (c) **Two**: failures are not cached.
- A method whose result is `null` is cached too: a second call with the same key did not run the method.

**Task 2.**
- `@CacheInvalidate` on `change(String currency, BigDecimal newRate)` **without** `@CacheKey` invalidates nothing and raises no error: the key of the call is the pair (currency, newRate), which never matches an entry of `rateOf`, whose key is the currency alone. `aChangedRateIsVisibleImmediately` and `changingOneRateKeepsTheOthersCached` fail with `Expected: <2.0F> Actual: <1.0F>`: the old rate is served. With `@CacheKey` on `currency` both pass.
- `@CacheInvalidateAll` on `change` fixes the stale read but `changingOneRateKeepsTheOthersCached` fails with `Expected: <2> but: was <3>`: USD had to be fetched again.
- A write behind the cache stays invisible until the entry expires. With the TTL set to 1 second in the test profile it takes at most about a second; with no `expire-after-write` configured, `aWriteBehindTheCacheBecomesVisibleOnlyAfterTheTtl` waits its 5 seconds and fails: the stale rate is served indefinitely.

**Task 3.**
- Without `concurrentExecution = SKIP` the default is `PROCEED` and the test fails with `Expected: <1> but: was <2>`: the second run starts while the first one is still sleeping.
- Intervals are read from configuration with `every = "{rates.refresh.every}"`. In the test profile `rates.refresh.every=off` disables the job; `SchedulerTest` switches it on with a test profile (a configuration override).
- An interval below one second is not reliable. With `100ms` and a 300 ms job, the starter (which has the default `PROCEED`) never showed an overlap, because the scheduler only fires about once a second; the test passed for the wrong reason. The documentation says that a value below one second may not be supported by the scheduler implementation.
</details>

## 🎤 Self-assessment
1. What is the key of a `@CacheResult` method with three parameters, and how do you change it? What happens to `@CacheInvalidate` when its parameters differ from the cached method's?
2. A price changed in the database through a SQL script and the API still shows the old one for minutes. Name two causes and the fix for each.
3. Ten requests ask for the same uncached key at once. What does a naive cache do, what does Quarkus do, and what is the limit of that guarantee?
4. Why is `maximum-size` mandatory in practice, and what do you watch to know if the cache is worth having?
5. A `@Scheduled` job must run once per day. You deploy three instances. What happens, and what are two ways to make it run once?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
