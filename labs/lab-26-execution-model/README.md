# ⚙️ Lab 26 - Execution model

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
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead. No Docker is needed. The tests make the machine **small on purpose**: one event loop, four worker threads and two threads to carry virtual threads (the profile `ExecutionProfile` and the `pom.xml`). With the defaults of a laptop (this one has 32 cores) every problem of this lab would hide.

## 🎯 Why it matters
A Quarkus service serves thousands of connections with a handful of threads, and that only works if every piece of code runs on the **right kind** of thread. Run a blocking call on the wrong one and a single slow database query freezes unrelated requests while the CPU sits idle; run out of the right one and requests queue behind each other for no reason. "The service is slow and the CPU is at 10%" is a classic incident, and "what does this method run on?" is a classic interview question.

## 🧠 Concepts
| Thread | How many | What it is for | Rule |
|---|---|---|---|
| **Event loop** (IO thread) | a few (`quarkus.vertx.event-loops-pool-size`) | Reading and writing bytes, and running **non-blocking** code | Never wait here: while it waits, every connection it serves waits |
| **Worker** | a pool (`quarkus.thread-pool.max-threads`) | **Blocking** code: JDBC, files, old clients, `Thread.sleep` | One thread per request in progress: the pool is the limit |
| **Virtual thread** (Java 21) | one per request, cheap | **Blocking-style** code that waits a lot | Waiting frees the carrier thread, unless it is *pinned* |

Quarkus REST decides where an endpoint runs from its **return type**: a method that returns `Uni`, `Multi`, `CompletionStage` or `Publisher` runs on the event loop; anything else runs on a worker. Three annotations override that guess: `@Blocking` (force a worker), `@NonBlocking` (force the event loop) and `@RunOnVirtualThread` (a new virtual thread per call; only for endpoints that are blocking).

```mermaid
flowchart LR
    R[Request bytes] --> E[Event loop reads and parses]
    E --> D{Endpoint is...}
    D -->|non-blocking: Uni, @NonBlocking| N[Runs on the event loop]
    D -->|blocking: plain return, @Blocking| W[Handed to a worker thread]
    D -->|@RunOnVirtualThread| V[Handed to a new virtual thread]
    N --> O[Answer written by the event loop]
    W --> O
    V --> O
```

## 📚 Documentation
- [Quarkus REST: execution model, blocking, non-blocking](https://quarkus.io/guides/rest)
- [Virtual threads in Quarkus](https://quarkus.io/guides/virtual-threads) (including the pinning cases)
- [Quarkus reactive architecture](https://quarkus.io/guides/quarkus-reactive-architecture)
- [Mutiny primer](https://quarkus.io/guides/mutiny-primer) (lab 27 owns it; here `Uni` is only a return type)
- [All configuration properties](https://quarkus.io/guides/all-config) (`quarkus.thread-pool.*`, `quarkus.vertx.*`, `quarkus.virtual-threads.*`)
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444) and [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491)

## 🧭 Scenario
A tiny price service on top of a **slow, blocking driver** (`LegacyCatalog`: it stands for JDBC or an old client, and every call waits 300 ms). Already written: the data model, the driver, a `Where` object that says which kind of thread built an answer, the two resources and the tests. **Your job is to make every endpoint run where it should.**

| Task | What must be true |
|---|---|
| 1 | Five endpoints that answer `where they ran`: each one runs on the kind of thread that suits its work |
| 2 | While slow lookups are in progress the event loop stays free, and a total that needs the price works |
| 3 | Sixteen requests that wait for the driver at once finish in about one wait, with only four workers |
| 4 | The cache in front of the driver does not defeat virtual threads |

## ✅ Tasks

### Task 1 - Where does it run?
**Goal:** every `/where/*` endpoint runs on the right kind of thread. **Validated by:** `ThreadKindTest`.

| Endpoint | Returns | Its work | Must run on |
|---|---|---|---|
| `/where/plain` | an object | nothing special | a worker |
| `/where/uni` | a `Uni` | nothing special | the event loop |
| `/where/legacy` | a `Uni` | calls the blocking driver | a worker |
| `/where/cheap` | an object | a few operations, no waiting | the event loop |
| `/where/virtual` | an object | calls the blocking driver, many at once | a virtual thread |

- [ ] Run the test class and read which cases fail. Two of the five are right as they are.
- [ ] Fix the other three in `WhereResource` (look for the `TODO`s).

```powershell
.\mvnw.cmd test '-Dtest=ThreadKindTest'
```

```bash
./mvnw test -Dtest=ThreadKindTest
```

🔮 **Predict first:** before running anything, write down for each of the five endpoints which kind of thread you expect (event loop, worker or virtual), and which two are right without a single change.

> [!WARNING]
> Returning a `Uni` does **not** make a method non-blocking: it only tells the framework to run it on the event loop. If the code inside waits, the event loop waits.

### Task 2 - Do not freeze the event loop
**Goal:** a slow lookup must not delay unrelated requests, and a total must be computed. **Validated by:** `EventLoopTest`.

- [ ] `GET /prices/{sku}` returns a `Uni<Price>` and calls the slow driver. Run the test class: while three lookups are in progress, how long does a `/ping` wait?
- [ ] `GET /prices/{sku}/total` needs the price to multiply it by a quantity, so it waits for the `Uni`. Read the error in the console and in the response.
- [ ] Fix both in `PriceResource` (look for the `TODO`s).

```powershell
.\mvnw.cmd test '-Dtest=EventLoopTest'
```

```bash
./mvnw test -Dtest=EventLoopTest
```

🔮 **Predict first:** (a) with one event loop and three slow lookups in flight, how long do you expect a `/ping` to take: about 10 ms, about 300 ms, or about 900 ms? (b) when a method that runs on the event loop calls `.await().indefinitely()` on a `Uni`, does Quarkus let it wait?

> [!WARNING]
> The event loop that serves the slow endpoint also serves `/ping`, which has nothing to do with it. The service is "up", the CPU is idle, and everybody is slow.

### Task 3 - Many waits, few threads
**Goal:** sixteen requests that wait for the driver at the same time finish in about one wait. **Validated by:** `VirtualThreadTest`.

- [ ] `GET /prices/{sku}/blocking` is a plain blocking endpoint: it runs on a worker, and the tests give the pool four. Run the test class and see how long sixteen requests take.
- [ ] Make it run on a virtual thread (look for the `TODO`).

```powershell
.\mvnw.cmd test '-Dtest=VirtualThreadTest'
```

```bash
./mvnw test -Dtest=VirtualThreadTest
```

🔮 **Predict first:** sixteen requests, 300 ms each, four workers. How long do they take on workers? And on virtual threads?

### Task 4 - The cache that defeats the virtual threads
**Goal:** the cached endpoint scales like the previous one. **Validated by:** `PinningTest`.

- [ ] `GET /prices/{sku}/cached` already runs on virtual threads, through a cache with one lock per SKU that is held while the driver answers. Run the test class: sixteen *different* SKUs, and it is slow anyway. Why?
- [ ] Fix `PriceCache`. Two tests are guards that already pass: after your fix they must still pass (one call to the driver per SKU, however many requests arrive together).

```powershell
.\mvnw.cmd test '-Dtest=PinningTest'
```

```bash
./mvnw test -Dtest=PinningTest
```

🔮 **Predict first:** the code holds a lock only per SKU, so no two of the sixteen requests ever wait for each other's lock. What could still make them wait?

> [!WARNING]
> This task depends on the Java version. The lab runs on **Java 21**, where a virtual thread that blocks inside a `synchronized` block keeps its carrier thread. Java 24 and later (JEP 491) removed that case. On a newer JDK the test passes before you touch anything: read the JEPs and carry on.

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
While Task 2 is red, the console of the test run shows the event loop complaining. The limit is 200 ms in these tests (2 seconds by default):

```text
WARN  [io.vertx.core.impl.BlockedThreadChecker] (vertx-blocked-thread-checker) Thread Thread[vert.x-eventloop-thread-0,5,main] has been blocked for 298 ms, time limit is 200 ms: io.vertx.core.VertxException: Thread blocked
```

and the totals answer a `500` and the console says:

```text
ERROR [io.quarkus.vertx.http.runtime.QuarkusErrorHandler] (vert.x-eventloop-thread-0) HTTP Request to /prices/A-2/total?quantity=2 failed, error id: ...: java.lang.IllegalStateException: The current thread cannot be blocked: vert.x-eventloop-thread-0
```

A finished `/where/uni` answers `{"thread":"vert.x-eventloop-thread-0","kind":"EVENT_LOOP","blockingAllowed":false}` and a finished `/where/virtual` answers `{"thread":"quarkus-virtual-thread-0","kind":"VIRTUAL","blockingAllowed":true}`.

## 💼 In real projects
- **The default is a good default.** Classic blocking code (JDBC, Hibernate ORM, blocking clients) belongs on workers, and a method that returns the result directly gets that for free. Do not wrap blocking calls in a `Uni` to look reactive: it moves them *onto* the event loop.
- **The event loop is for non-blocking work and short computations.** Nothing there may wait: no `Thread.sleep`, no JDBC, no file reads, no `await()`. Quarkus protects you in some places (the error of Task 2) and only complains in others (the blocked-thread warning, after `quarkus.vertx.max-event-loop-execute-time`, 2 seconds by default). In production that log line is the alarm, and a latency that grows for *unrelated* endpoints is its symptom.
- **Virtual threads shine when code is written in blocking style and spends most of its time waiting** (calls to slow remote services), and they are explicit in Quarkus on purpose. The official guide lists the reasons not to use them everywhere: **pinning**, **monopolization** of the carriers by long computations, and libraries that cache objects in `ThreadLocal`s (one virtual thread per request means one new object per request). Check the drivers you depend on, and measure.
- **Pinning on Java 21 comes from `synchronized`** (and native frames). The code that blocks inside a monitor can be your own or a library's. Prefer `ReentrantLock` where you block while holding a lock, and never hold one **while calling a slow service** unless it has to be: a single global lock serializes everything on any kind of thread (see the mutation table).
- **Sizes have no fixed default** (the properties `quarkus.vertx.event-loops-pool-size` and `quarkus.thread-pool.max-threads` are unset), so they are computed when the application starts. Look at the values on the machine you deploy to, and set the worker pool for your load.
- **Context travels with the framework's threads**, not with yours (see the stretch goal): the request id of lab 25 is there on workers, the event loop and `@RunOnVirtualThread`, and gone in a thread you start yourself.
- **Tests that depend on threads need a small, fixed machine.** A profile that sets event loops, workers and carriers makes them behave the same on every laptop and in CI.
- A method annotated `@Transactional` is treated as blocking by the official guide, because JTA is a blocking technology (not exercised in this lab, which has no database).

## 🚀 Stretch goal
1. **The request id on every kind of thread.** Add a request filter that puts a `requestId` in the MDC (lab 25) and four endpoints that read it: on a worker, on the event loop, with `@RunOnVirtualThread`, and from a thread started with `Thread.ofVirtual()`. Predict the four answers, write a test, and compare with lab 25.
2. **An escape hatch.** `quarkus.virtual-threads.enabled=false` exists. Predict what `/where/virtual` answers with it, and run `ThreadKindTest` with `-Dquarkus.virtual-threads.enabled=false` (in PowerShell, quote it).
3. **Look at it from the inside.** Add `-Djdk.tracePinnedThreads=short` to the JVM that runs the tests and read what it prints for the unfixed cache.

## 💡 Hints
<details><summary>Task 1 (direction)</summary>

The three annotations live in `io.smallrye.common.annotation`: `Blocking`, `NonBlocking` and `RunOnVirtualThread`. They go on the method.
</details>

<details><summary>Task 1 (more concrete)</summary>

`/where/legacy` needs `@Blocking`, `/where/cheap` needs `@NonBlocking` and `/where/virtual` needs `@RunOnVirtualThread`.
</details>

<details><summary>Task 2 (direction)</summary>

The work has to leave the event loop. Which annotation of Task 1 does that for a method that returns a `Uni`? Once the method runs on a worker, waiting for the value is allowed.
</details>

<details><summary>Task 3 (direction)</summary>

The annotation of Task 1 that gives every call its own virtual thread.
</details>

<details><summary>Task 4 (direction)</summary>

Which operation of the cache blocks, and what is the thread holding when it does? `java.util.concurrent.locks.ReentrantLock` has the same effect as the block you have, without the monitor.
</details>

<details><summary>Task 4 (more concrete)</summary>

One `ReentrantLock` per SKU instead of one `Object`: `lock()` before reading the cache, `unlock()` in a `finally`. Do not use a single lock for every SKU.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 and Java 21 (on a machine with 32 cores, with the small profile of the tests). Times are from one machine: yours will differ, the proportions will not.

**Task 1.** The two that are right without a change are `plain` (a worker) and `uni` (the event loop). The three that were wrong: `legacy` ran on the event loop (and the blocked-thread warning appeared), `cheap` ran on a worker, `virtual` ran on a worker. With the fixes, `blockingAllowed` is `false` exactly on the event loop, and the thread names are `vert.x-eventloop-thread-N`, `executor-thread-N` and `quarkus-virtual-thread-N`.
A `Uni` mapped later (`Uni.createFrom().item(...).map(...)`) still ran its `map` on the event loop; with `@Blocking` on the method, the supplier of a `Uni` ran on a worker.

**Task 2.** (a) A `/ping` took **about 430 ms** while one blocking call held the only event loop (about 10 ms when idle); with three lookups in flight the test measured about 850 ms in the mutation without `@Blocking`. (b) No: `await()` on the event loop throws `java.lang.IllegalStateException: The current thread cannot be blocked: vert.x-eventloop-thread-0`, which is what turned a total into a 500. Quarkus refuses the wait because it would freeze the loop; it does **not** refuse `Thread.sleep` (the blocking call of this lab), which only produces the warning. `@Blocking` fixed both endpoints (it is the one fix I verified; any fix that moves the driver call off the event loop satisfies the tests).

**Task 3.** On four workers sixteen waits of 300 ms took **about 1.3 seconds** (four rounds); on virtual threads **about 0.36 seconds**. The workers were never "busy": they were waiting.

**Task 4.** With two carrier threads, sixteen waits of 300 ms inside `synchronized` took **about 2.5 seconds** (eight rounds of two); with `ReentrantLock`, **about 0.33 seconds**. With the 32 carriers this machine has by default the same `synchronized` code took 0.34 s: pinning is real but invisible until the carriers run out, which is why the `pom.xml` gives the tests two. Moving to one lock for all SKUs (not per SKU) made it **5 seconds**: the lock itself serialized the sixteen requests, whatever the kind of thread.

**Stretch 1.** The `requestId` was present on a worker, on the event loop and with `@RunOnVirtualThread`; with `Thread.ofVirtual().start(...)` it was `null` (the same as the plain executors of lab 25). **Stretch 2.** With the flag set to `false`, `/where/virtual` answers `{"thread":"executor-thread-1","kind":"WORKER",...}`: the annotation falls back to the worker pool.

**What each piece protects (mutation table).** Each row is the solution with one thing removed:

| Removed from the solution | What fails |
|---|---|
| `@Blocking` on `/where/legacy` | three cases of `ThreadKindTest` (it runs on the event loop) |
| `@NonBlocking` on `/where/cheap` | three cases of `ThreadKindTest` |
| `@Blocking` on `/prices/{sku}` | `slowLookupsDoNotFreezeTheOtherRequests`: a ping took 850 ms instead of under 200 |
| `@Blocking` on `/prices/{sku}/total` | two tests: `500` instead of `200` |
| `@RunOnVirtualThread` on `/blocking` | both tests of `VirtualThreadTest` (1.28 s instead of under 0.8) |
| `ReentrantLock` replaced by `synchronized` | `sixteenDifferentSkusThroughTheCacheFinishInAboutOneWait`: 2.5 s |
| the lock removed altogether | `theSameSkuAskedFourTimesAtOnceIsLookedUpOnce`: the driver was called 4 times |
| one lock for every SKU | `sixteenDifferentSkus...`: 5 s |
</details>

## 🎤 Self-assessment
1. An endpoint returns a `Uni` and, inside, calls JDBC. Where does it run, what happens to the requests of other endpoints, and how do you fix it?
2. Why does `await()` throw on the event loop while `Thread.sleep` does not, and what does that tell you about how much the framework protects you?
3. Four workers and sixteen requests that wait 300 ms: how long do they take on workers and on virtual threads, and why are the workers not "busy"?
4. What is pinning, in which Java versions does `synchronized` cause it, and why did it stay hidden on a 32-core machine until the tests limited the carriers?
5. How would you notice in production that the event loop is blocked, before a customer tells you? Which two settings control when the warning appears?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
